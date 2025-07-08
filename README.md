# Chords-Java
Chords parser written in Java, connects to development board running Chords Arduino Firmware.

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
│   │   └── chords/
│   │       └── usb/
│   │           └── ChordsUSB.java
│   └── edu/
│       └── ucsd/
│           └── sccn/
│               └── LSL.java
```

## 1. Download the latest version of `Java`

- Windows - https://download.oracle.com/java/24/latest/jdk-24_windows-x64_bin.exe
- Linux - https://download.oracle.com/java/24/latest/jdk-24_linux-x64_bin.deb
- macOS - https://download.oracle.com/java/24/latest/jdk-24_macos-x64_bin.dmg

## 2. Download our repository `Chords-Java` 

- Chords-Java from GitHub : https://github.com/upsidedownlabs/Chords-Java.git

## 3.  Prepare the Project Directories

Inside the  `ChordsLSLStreamer` folder, ensure you have two subfolders: 

- `src/` — for all `.java` files

- `lib/` — for all external `.jar` and native files

## 4. Add Source Files

### 4.1 Add `ChordsUSB.java`

- Place the `ChordsUSB.java` file inside the `src/com/chords/usb/` folder of your `ChordsLSLStreamer` project.

### 4.2 Download the `Java LSL JAR`

1. Download the Java LSL bindings source from: https://github.com/labstreaminglayer/liblsl-Java
        
2. Click the green "Code" button → Download ZIP
        
3. Unzip the archive. Navigate to:`liblsl-java-master/src/edu` and copy the entire `edu` folder into your `src/` in your `ChordsLSLStreamer` folder.

### 4.3 Add the Java Source file

- Place the `ChordsLSLStreamer.java` file from the repository into the `src/` folder in your `ChordsLSLStreamer` folder.
- Open the terminal(or command prompt) in `src/` folder in your project.

## 5. Download Dependencies

### 5.1 Download jSerialComm Library

1. Visit the official Maven Central repository: https://repo1.maven.org/maven2/com/fazecast/jSerialComm/2.11.0/

2. Download the file: `jserialcomm-2.11.0.jar`

3. Place the downloaded `.jar` file inside the `lib/` folder of your `ChordsLSLStreamer` project.

**Download and Include all libraries and native libraries for LSL**   

### 5.2 LSL Native library (`liblsl.dll`)

1. Visit: https://github.com/sccn/liblsl/releases

2. Download the ZIP archive for Windows: `liblsl-1.16.2-Win_amd64.zip`

3. Extract the `liblsl64.dll` from the `bin/` directory
             
**Note: You can have different name like `lsl.dll` in this case you have to change the name to "liblsl64.dll"**        

4. Place `liblsl64.dll` in the `lib` folder in your directory `ChordsLSLStreamer`.

### 5.3 JNA(Java Native Access).jar files

1. Download these two JARs:
            
- `jna-5.13.0.jar`: https://mvnrepository.com/artifact/net.java.dev.jna/jna/5.13.0
- `jna-platform-5.13.0.jar`: https://mvnrepository.com/artifact/net.java.dev.jna/jna-platform/5.13.0

2. Place the downloaded JARs in the `lib` folder in your directory.

## 6. Compile the Project

### 6.1 To Compile, Open a terminal in `ChordsLSLStreamer/src` and run:

`javac -cp "../lib/*" com\chords\usb\ChordsUSB.java edu\ucsd\sccn\LSL.java ChordsLSLStreamer.java`


## 7. Run the Project

`java -cp ".;../lib/*" ChordsLSLStreamer`

- If everything works you will see: `[? Started LSL stream: CHORDS_USB_Stream]`

**The Streaming Project is completed. Now download `Open Ephys` to visualize the real-time multi-channel data in graphical form**

## 8. Visualize Data in Open Ephys
 
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