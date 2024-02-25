 # Xmp Mod Player

 Xmp Mod Player is an Android app that plays over 90 mainstream and obscure module formats, including Protracker (MOD), Scream Tracker 3 (S3M), Fast Tracker II (XM) and Impulse Tracker (IT), and more!

Check out [libxmp](https://github.com/libxmp/libxmp) which does all the heavy lifting to play modules.
</br></br>

### This is a unofficial fan made revival
---
Since android has changed so much over the past 5+ years. The original app has fallen behind on modern development standards, making it crash prone, dated in looks, and uses an old version of libxmp which has gotten much love since the latest version was posted on the Play Store.

The original/official app is made by [Claudio Matsuoka](https://github.com/cmatsuoka/xmp-android) and can still be found on the Google Play Store [Xmp Mod Player](https://play.google.com/store/apps/details?id=org.helllabs.android.xmp)

*This fork is a playground to a degree. Things may change or break depending on the idea being explored.*
</br></br>

### Features and Changes
---
- Support for the latest android sdk versions
- Storage Access Framework - Mods and Playlists will now be under one directory instead of two separate folders in your root storage
- A complete UI overhaul using Jetpack Compose
- New json based playlist - Old playlist migration planned
- (Planned) oboe - A high performance audio library using OpenSL or AAudio
- (Planned) media3 - A media support library for a rich media experience

Full list of dependencies can be found [here](https://github.com/LossyDragon/xmp-android/blob/2023/gradle/libs.versions.toml)
</br></br>

### Building the app
---
1. Make sure to have an up to date version of Android Studio (Iguana | 2023.2.1) or newer
2. Git clone [libxmp](https://github.com/libxmp/libxmp) into `app\src\main\jni`
3. (Optional) If you plan to use the downloading feature, make sure to have a valid API key from [The Mod Archive](https://modarchive.org/). Add your key to `modArchiveApiKey=....`  which will need to be in your gradle.properties (Usually in your global C:\\Users\\username\\.gradle)
4. Build and Launch

Note: Debug builds using Jetpack Compose will have slim-to-severe performance issues.
