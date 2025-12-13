# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Vosk classes
-keep class org.vosk.** { *; }

# Keep location services
-keep class com.google.android.gms.location.** { *; }
