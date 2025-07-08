package myapp;

import com.chords.usb.ChordsUSB;


public class TestApp {
    public static void main(String[] args) {
        ChordsUSB chords = new ChordsUSB();

        if (!chords.detect_hardware()) {
            System.out.println("Device not detected.");
            return;
        }

        chords.setSampleMissListener(missed -> System.err.println("Missed packets: " + missed));
        chords.startStreaming();

        // Print samples
        new Thread(() -> {
            while (true) {
                int numChannels = chords.getNumChannels();
                float[][] all = new float[numChannels][];
                for (int i = 0; i < numChannels; i++) all[i] = chords.getChannelBuffer(i);
                int lastIndex = all[0].length - 1;
                for (int i = 0; i < numChannels; i++) {
                    System.out.printf("CH%d: %.2f  ", i + 1, all[i][lastIndex]);
                }
                System.out.println();
                try {
                    Thread.sleep(1000 / chords.getSamplingRate());
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}

