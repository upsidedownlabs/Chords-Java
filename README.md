# Chords-Java
Chords parser written in Java, connects to development board running [Chords Arduino Firmware](https://github.com/upsidedownlabs/Chords-Arduino-Firmware).

## Features
   
- Detects and connects to the compatible USB devices.
- Streams real-time multi-channel BioAmp data.
- Detects missed samples and packetloss.

## 📁 Project Folder Structure

```plaintext
ChordsLSLStreamer/
├── lib/
│   ├── jserialcomm-2.11.0.jar
│   ├── jna.jar
│   ├── jna-platform.jar
│   └── liblsl64.dll
│
├── src/
│   ├── ChordsLSLStreamer.java
│   ├── com/
│       └── chords/
│           └── usb/
│              └── ChordsUSB.java
│   
```

## 1. Download the latest version of `Java`

- Windows - https://download.oracle.com/java/24/latest/jdk-24_windows-x64_bin.exe
- Linux - https://download.oracle.com/java/24/latest/jdk-24_linux-x64_bin.deb
- macOS - https://download.oracle.com/java/24/latest/jdk-24_macos-x64_bin.dmg

## 2. Download our repository `Chords-Java` 

- Chords-Java from GitHub : `https://github.com/upsidedownlabs/Chords-Java.git`

## 3. Compile the Project

### 3.1 To Compile, Open a terminal in downloaded repo and run:

`javac -d bin -cp "lib/*" src/ChordsUSB.java examples/ChordsLSLStreamer.java`

## 4. Run the Project

`java '-Djna.library.path=lib' -cp "bin;lib/*" ChordsLSLStreamer`

- If everything works you will see: `[? Started LSL stream: CHORDS_USB_Stream]`

**The Streaming Project is completed. Now download `Open Ephys` to visualize the real-time multi-channel data in graphical form**

## 5. Visualize Data in Open Ephys
 
- Download and Install `Open Ephys` GUI from: https://open-ephys.org/gui

**Pre requisites for `Open Ephys`**

- You should have C++ downloaded on your device (recommended via Visual Studio) [https://visualstudio.microsoft.com/vs/features/cplusplus/].
- Set up the `Open Ephys` and run it. (If you get a Windows warning, click "More info" > "Run anyway")

### Execution of Open Ephys

When the Open Ephys window opens.

1. Go to File menu on top left corner.

2. Select `Plugin Installer`, a plugin installer window will open.

3. Now install the `Lib Streaming Layer IO`(LSL Inlet).You will find it in the `Source` option.

4. Now drag the `LSL Inlet` to the `Signal Chain` window.

5. Also drag `Merger` and `LPF Viewer`.

6. Start the stream from `ChordsLSLStreamer`, there will be an option in the `LSL Inlet` to select the stream `CHORDS_USB_Stream`

7. Now in the top-right side there is a play button `▶️`, click the button.
(In the LPF Viewer, click the "Open visualizer in window" icon in the top-right).
Now you can see the real-time multi channel graphs of data.

8. Select the range to 2000 for appropriate view of all the channels.

## Further Resources

- To learn more about `Open Ephys` visit [https://open-ephys.org/doc-overview].
