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
First of all quit the Elgato Stream Deck app or any other app that can connect to the Stream Deck (e.g. Hammerspoon, BetterTouchTool).

After the project is built, you can run it like this:
```shell
java -jar build/libs/StreamDeckVideoPlayer-1.0-SNAPSHOT.jar "$HOME/Downloads/bad_apple_120.mp4"
```

Note the last argument – it should be a path to an existing video file.
