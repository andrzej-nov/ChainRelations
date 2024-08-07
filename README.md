# Chain Relations

**Chain Relations** is a small casual existential game about life, human needs and long-term relations.

![Clipboard01!](https://user-images.githubusercontent.com/89737218/164009636-a513f32f-f540-4b04-8dac-d9f49f459ee0.jpg)

https://user-images.githubusercontent.com/89737218/164009583-ab634b5f-f95e-4472-8736-b11c3577d8f2.mp4

**Game objective:** connect the balls' same-colored inputs to outputs. Completely connected balls disappear.
The possible connection range is limited, so plan your moves.

You can adjust balls count, connection range, whether the balls are respawned, and put additional color to the board.

Hint: from time to time balls blink and randomly change their colors one by one, so if you are out of the moves,
you can just wait a bit.

The game is intended to be meditative, so there is no game timer and no hall of fame. There are
also no sound, no ads and no in-game purchases. The game is completely free and will remain so. It does not use Internet
connection and does not require any device permissions.

The game is auto-saved every 5 seconds, so if you close it immediately after a move, you might lose up to last 5 seconds
of the game.

## Download

The game is provided in two options:

- **Desktop Java**. [Download ChainRelations.jar](https://github.com/andrzej-nov/ChainRelations/releases/download/v1.8/ChainRelations.jar).
  Run it with `java -jar ChainRelations.jar` command line, or in most cases just double-click the ChainRelations.jar 
  file. It has been tested with Java 18 Runtime, should also work with prior versions up to Java 8.
    - **On MacOS** you will get a warning about unidentified developer. Start the ChainRelations.jar 
      using Finder context menu instead of Launchpad,
      [as explained here](https://www.bemidjistate.edu/offices/its/knowledge-base/how-to-open-an-app-from-an-unidentified-developer-and-exempt-it-from-gatekeeper/)
      .

- **Android**. [Get it on Google Play](https://play.google.com/store/apps/details?id=com.andrzejn.chainrelations)
  (recommended) or
  [download the ChainRelations.apk](https://github.com/andrzej-nov/ChainRelations/releases/download/v1.8/ChainRelations.apk)
  here for manual install (it might be sometimes also a newer version than on the Google Play). It has been tested
  on Android 8.0 and 10.0, should also work on any Android version starting from 4.4 and later.

There is no iOS build because I do not have tools to test and deploy it to the AppStore. If somebody completes the iOS
module (see below), I will add it here.

## Donation

If you like the game and want to support the author, you may donate arbitrary amount via following
link: https://pay.fondy.eu/s/3m1mZ2KQsJtpYTN (processed by the [Fondy.eu](https://fondy.io/) payment system).

## Development

The game is provided under the [Creative Commons Attribution license](https://creativecommons.org/licenses/by/4.0/).
Please feel free to reuse, extend, derive, improve etc. as long as you keep a reference to the original and mention me,
Andrzej Novosiolov, as the original author.

The game has been implemented using following tools and libraries:

- [IntelliJ IDEA 2024.1.4 (Community Edition)](https://www.jetbrains.com/idea/download/)
- [Android Studio 2024.1.1 Patch 1](https://developer.android.com/studio) (for the Android emulator)
- [Gradle 8.2.2](https://gradle.org/)
- [Kotlin 2.0.0](https://kotlinlang.org/)
- [libGDX 1.12.1](https://libgdx.com/)
- [libKTX 1.12.1-rc1](https://libktx.github.io/)
- [ShapeDrawer 2.6.0](https://github.com/earlygrey/shapedrawer#shape-drawer)
- [Universal Tween Engine 6.3.3](https://github.com/AurelienRibon/universal-tween-engine)
- Free icons from https://www.flaticon.com/

The `ios` module is present in the project and compiling, but I did not tested it because I do not have Apple devices
and tools for that. If you make it work, I would gratefully accept the pull request.
