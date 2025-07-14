

import edu.ucsd.sccn.LSL;

import java.util.Arrays;

public class ChordsLSLStreamer {

    public static void main(String[] args) {
        ChordsUSB chords = new ChordsUSB();
        if (!chords.detect_hardware()) {
            System.err.println("❌ No CHORDS device found.");
            return;
        }

        int numChannels = chords.getNumChannels();
        int samplingRate = chords.getSamplingRate();
          int bits = chords.getBitResolution(); 
        String boardName = chords.getBoardType();

        System.out.println("✅ Connected to: " + boardName + ", " + numChannels + " channels @ " + samplingRate + " Hz");

        chords.startStreaming();

        try {
            LSL.StreamInfo info = new LSL.StreamInfo(
                "CHORDS_USB_Stream",
                "EEG",
                numChannels,
                samplingRate,
                LSL.ChannelFormat.float32,
                "chords-" + boardName
            );

            LSL.XMLElement channels = info.desc().append_child("channels");
            for (int i = 0; i < numChannels; i++) {
                channels.append_child("channel")
                        .append_child_value("label", "CH" + (i + 1))
                        .append_child_value("unit", "microvolts")
                        .append_child_value("type", "EEG");
            }

            LSL.XMLElement resinfo = info.desc().append_child("resinfo");
            resinfo.append_child_value("resolution", String.valueOf(bits));
           

            LSL.StreamOutlet outlet = new LSL.StreamOutlet(info);
            System.out.println("🚀 Started LSL stream: CHORDS_USB_Stream");

            // ✅ Streaming thread
            Thread streamThread = new Thread(() -> {
                final int chunkSize = 10; // adjust this for faster/slower wave scroll
                final float gain = 1.0f;  // ✅ amplification multiplier

                final long nanosPerSample = 1_000_000_000L / samplingRate;
                final long nanosPerChunk = nanosPerSample * chunkSize;
                long lastPushTime = System.nanoTime();

                while (true) {
                    float[][] chunk = chords.getRecentSamples(chunkSize);

                    if (chunk != null && chunk.length == chunkSize) {
                        for (int i = 0; i < chunk.length; i++) {
                            for (int j = 0; j < chunk[i].length; j++) {
                                chunk[i][j] *= gain; // ✅ apply gain
                            }
                            outlet.push_sample(chunk[i]); // ✅ send to LSL
                        }

                        // Optional: print last sample
                        System.out.println(Arrays.toString(chunk[chunk.length - 1]));

                        // Timing control for real-time accuracy
                        long now = System.nanoTime();
                        long timeElapsed = now - lastPushTime;
                        long sleepNanos = nanosPerChunk - timeElapsed;

                        if (sleepNanos > 0) {
                            try {
                                Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                            } catch (InterruptedException ignored) {}
                        }

                        lastPushTime = System.nanoTime();
                    }
                }
            });

            streamThread.setDaemon(true);
            streamThread.start();

            System.out.println("🔄 Streaming... Press Ctrl+C to stop.");
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            chords.stopStreaming();
            System.out.println("🛑 CHORDS stream stopped.");
        }
    }
}