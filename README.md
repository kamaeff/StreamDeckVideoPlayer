# Stream Deck Video Player

It's a simple app which can play videos on your stream deck in full screen mode.  
Only Stream Deck Mk2 currently supported. 

## Build

I only built it with OpenJDK 23 on macOS.  
I suppose that other JDKs and operating systems are OK too.

To build the project, run:
```shell
./gradlew jar
```


## Run
First of all quit Elgato Stream Deck app or any other app that can connect to Stream Deck (e.g. Hammerspoon, BetterTouchTool).

After project is built, tou can run it like this:
```shell
java -jar build/libs/StreamDeckVideoPlayer-1.0-SNAPSHOT.jar "/path/to/your.video.mp4"
```

Note the last argument – it should be a path to existing video file.
