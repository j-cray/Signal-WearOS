# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/icarus/Android/Sdk/tools/proguard/proguard-android-optimize.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep our main entry points
-keep class com.example.signalwearos.presentation.MainActivity { *; }
-keep class com.example.signalwearos.SignalApplication { *; }
-keep class com.example.signalwearos.complication.MainComplicationService { *; }
-keep class com.example.signalwearos.tile.MainTileService { *; }

# Keep Signal Client classes
-keep class org.signal.libsignal.** { *; }
-keep class com.example.signalwearos.data.signal.** { *; }

# Keep Room entities and DAOs
-keep class com.example.signalwearos.data.db.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
