

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChordsUSB {

    public static final byte SYNC_BYTE1 = (byte) 0xC7;
    public static final byte SYNC_BYTE2 = (byte) 0x7C;
    public static final byte END_BYTE = 0x01;
    public static final int HEADER_LENGTH = 3;

    private SerialPort serialPort;
    private byte[] buffer = new byte[4096];
    private int bufferLength = 0;

    private String boardType;
    private int numChannels;
    private int packetLength;
    private float[][] data;

    private volatile boolean streaming = false;

    private int lastPacketCounter = -1;
    private boolean sampleMissed = false;
    private int sampleMissedTimeout = 0;
    private SampleMissListener missListener;

    private int totalPacketsReceived = 0;
    private int missedPackets = 0;

    // NEW: Recent Sample Buffer
    private float[][] recentSamples;
    private int writeIndex = 0;
    private int sampleCount = 0;
    private final int MAX_BUFFER_SIZE = 2048;

    public static class BoardInfo {
        int samplingRate;
        int numChannels;
        int bits;

        public BoardInfo(int samplingRate, int numChannels, int bits) {
            this.samplingRate = samplingRate;
            this.numChannels = numChannels;
            this.bits = bits;
        }
    }

    private static final Map<String, BoardInfo> supportedBoards = Map.ofEntries(
            Map.entry("UNO-R3", new BoardInfo(250, 6, 10)),
            Map.entry("UNO-CLONE", new BoardInfo(250, 6, 10)),
            Map.entry("GENUINO-UNO", new BoardInfo(250, 6, 10)),
            Map.entry("UNO-R4", new BoardInfo(500, 6, 14)),         // R4 uses Renesas RA4M1 (14-bit ADC)
            Map.entry("GIGA-R1", new BoardInfo(500, 6, 16)),        // GIGA R1 has 12-bit ADC
            Map.entry("RPI-PICO-RP2040", new BoardInfo(500, 3, 12)),// RP2040 has 12-bit ADC
            Map.entry("NPG-LITE", new BoardInfo(500, 3, 12)),       // Assuming based on RP2040
            Map.entry("NANO-CLONE", new BoardInfo(250, 8, 10)),
            Map.entry("NANO-CLASSIC", new BoardInfo(250, 8, 10)),
            Map.entry("STM32F4-BLACK-PILL", new BoardInfo(500, 8, 12)), // STM32F401 has 12-bit ADC
            Map.entry("STM32G4-CORE-BOARD", new BoardInfo(500, 16, 12)),// STM32G4 has 12-bit ADC
            Map.entry("MEGA-2560-R3", new BoardInfo(250, 16, 10)),
            Map.entry("MEGA-2560-CLONE", new BoardInfo(250, 16, 10))
    );

    

    public boolean detect_hardware() {
        int[] baudrates = {230400, 115200};
        SerialPort[] ports = SerialPort.getCommPorts();

        for (SerialPort port : ports) {
            for (int baud : baudrates) {
                if (tryConnect(port, baud)) {
                    return true;
                }
            }
        }

        System.out.println("No supported CHORDS board detected.");
        return false;
    }

    private boolean tryConnect(SerialPort port, int baudrate) {
        try {
            port.setBaudRate(baudrate);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 1000);
            if (!port.openPort()) return false;

            this.serialPort = port;

            for (int i = 0; i < 4; i++) {
                sendCommand("WHORU");
                String response = readLine();
                if (response != null && supportedBoards.containsKey(response)) {
                    this.boardType = response;
                    BoardInfo info = supportedBoards.get(response);
                    this.numChannels = info.numChannels;
                    this.packetLength = HEADER_LENGTH + 2 * numChannels + 1;
                    this.data = new float[numChannels][2000];

                    // NEW: Init circular sample buffer
                    this.recentSamples = new float[MAX_BUFFER_SIZE][numChannels];
                    this.writeIndex = 0;
                    this.sampleCount = 0;

                    System.out.println("Connected: " + boardType + " at " + port.getSystemPortName() + " @" + baudrate);
                    return true;
                }
            }

            port.closePort();
        } catch (Exception e) {
            System.err.println("Error connecting: " + e.getMessage());
        }
        return false;
    }

    public void setSampleMissListener(SampleMissListener listener) {
        missListener = listener;
    }

    private void sendCommand(String cmd) {
        try {
            serialPort.writeBytes((cmd + "\n").getBytes(), cmd.length() + 1);
        } catch (Exception e) {
            System.err.println("Send error: " + e.getMessage());
        }
    }

    private String readLine() {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[1];

        try {
            while (true) {
                if (serialPort.readBytes(buf, 1) <= 0) break;
                if (buf[0] == '\n') break;
                if (buf[0] != '\r') sb.append((char) buf[0]);
            }
        } catch (Exception e) {
            return null;
        }

        return sb.toString().trim();
    }

    public void startStreaming() {
        if (serialPort == null || !serialPort.isOpen()) {
            System.out.println("Port is not open. Cannot start streaming.");
            return;
        }

        sendCommand("START");
        streaming = true;

        Thread streamThread = new Thread(() -> {
            while (streaming) {
                readData();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        streamThread.setDaemon(true);
        streamThread.start();
    }

    public void stopStreaming() {
        streaming = false;
        sendCommand("STOP");
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }

    public void readData() {
        try {
            int available = serialPort.bytesAvailable();
            if (available > 0) {
                byte[] incoming = new byte[available];
                int read = serialPort.readBytes(incoming, available);
                System.arraycopy(incoming, 0, buffer, bufferLength, read);
                bufferLength += read;
            }

            while (bufferLength >= packetLength) {
                int syncIndex = findSyncIndex(buffer, bufferLength);
                if (syncIndex == -1) {
                    bufferLength = 0;
                    break;
                }

                if (bufferLength >= syncIndex + packetLength) {
                    byte[] packet = Arrays.copyOfRange(buffer, syncIndex, syncIndex + packetLength);
                    if (packet[0] == SYNC_BYTE1 && packet[1] == SYNC_BYTE2 && packet[packet.length - 1] == END_BYTE) {
                        processPacket(packet);

                        int next = syncIndex + packetLength;
                        System.arraycopy(buffer, next, buffer, 0, bufferLength - next);
                        bufferLength -= next;
                    } else {
                        syncIndex++;
                        System.arraycopy(buffer, syncIndex, buffer, 0, bufferLength - syncIndex);
                        bufferLength -= syncIndex;
                    }
                } else break;
            }
        } catch (Exception e) {
            System.err.println("Read error: " + e.getMessage());
        }
    }

    private void processPacket(byte[] packet) {
        int counter = packet[2] & 0xFF;
        totalPacketsReceived++;

        if (lastPacketCounter != -1) {
            int expected = (lastPacketCounter + 1) % 256;
            int missed = (counter - expected + 256) % 256;

            if (missed > 0 && missListener != null) {
                missListener.onSampleMiss(missed);
                missedPackets += missed;
                sampleMissed = true;
                sampleMissedTimeout = 5;
                System.err.println("Missed " + missed + " packets");
            } else if (sampleMissedTimeout > 0) {
                sampleMissedTimeout--;
            } else {
                sampleMissed = false;
            }
        }
        lastPacketCounter = counter;

        float[] currentSample = new float[numChannels];
        for (int i = 0; i < numChannels; i++) {
            int index = HEADER_LENGTH + 2 * i;
            int high = packet[index] & 0xFF;
            int low = packet[index + 1] & 0xFF;
            float sample = (high << 8) | low;

            System.arraycopy(data[i], 1, data[i], 0, data[i].length - 1);
            data[i][data[i].length - 1] = sample;
            currentSample[i] = sample;
        }

        // Store sample in circular buffer
        recentSamples[writeIndex] = currentSample;
        writeIndex = (writeIndex + 1) % MAX_BUFFER_SIZE;
        if (sampleCount < MAX_BUFFER_SIZE) sampleCount++;
    }

    private int findSyncIndex(byte[] buf, int len) {
        for (int i = 0; i < len - 1; i++) {
            if (buf[i] == SYNC_BYTE1 && buf[i + 1] == SYNC_BYTE2) return i;
        }
        return -1;
    }

    public float[] getChannelBuffer(int channel) {
        return (channel >= 0 && channel < numChannels) ? data[channel] : new float[0];
    }

    public int getNumChannels() {
        return numChannels;
    }

    public String getBoardType() {
        return boardType;
    }

    public int getSamplingRate() {
        BoardInfo info = supportedBoards.get(boardType);
        return info != null ? info.samplingRate : 0;
    }

    public int getBitResolution() {
    BoardInfo info = supportedBoards.get(boardType);
    return info != null ? info.bits : 0;
}

    public boolean isSampleMissing() {
        return sampleMissed;
    }

    public int getMissedPacketCount() {
        return missedPackets;
    }

    public int getTotalPacketsReceived() {
        return totalPacketsReceived;
    }

    public float getPacketLossPercentage() {
        int total = totalPacketsReceived + missedPackets;
        return total > 0 ? (100f * missedPackets) / total : 0;
    }

    public void refreshStats() {
        // Optional future use
    }

    // ✅ NEW: Method to retrieve recent synchronized samples
    public float[][] getRecentSamples(int n) {
        int available = Math.min(n, sampleCount);
        float[][] out = new float[available][numChannels];
        for (int i = 0; i < available; i++) {
            int idx = (writeIndex - available + i + MAX_BUFFER_SIZE) % MAX_BUFFER_SIZE;
            System.arraycopy(recentSamples[idx], 0, out[i], 0, numChannels);
        }
        return out;
    }

    // ✅ Embedded SampleMissListener interface
    public interface SampleMissListener {
        void onSampleMiss(int missedCount);
    }
}
