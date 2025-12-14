# Protector App - Production ProGuard Rules
# Comprehensive obfuscation while preserving required functionality

# ===== Android Components =====
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ===== Google Play Services =====
# Keep location services
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Keep wearable API
-keep class com.google.android.gms.wearable.** { *; }
-dontwarn com.google.android.gms.wearable.**

# Keep Play Services common
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.android.gms.common.**

# ===== Google Play Billing =====
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep billing interfaces and callbacks
-keep interface com.android.billingclient.api.** { *; }
-keep class com.android.billingclient.api.** { *; }

# ===== Vosk Speech Recognition =====
-keep class org.vosk.** { *; }
-dontwarn org.vosk.**

# Keep Vosk model classes
-keep class com.alphacephei.vosk.** { *; }
-dontwarn com.alphacephei.vosk.**

# ===== AndroidX =====
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ===== Application Classes =====
# Keep subscription-related classes (prevent tampering)
-keep class com.protector.app.billing.** { *; }

# Keep premium features (prevent feature unlock hacks)
-keep class com.protector.app.billing.PremiumFeatures {
    public static <methods>;
}

# Keep service classes
-keep class com.protector.app.service.** {
    public <methods>;
    public <fields>;
}

# Keep receiver classes
-keep class com.protector.app.receiver.** {
    public <methods>;
}

# Keep MainActivity (entry point)
-keep class com.protector.app.MainActivity {
    public <methods>;
}

# Keep SubscriptionActivity
-keep class com.protector.app.SubscriptionActivity {
    public <methods>;
}

# ===== Parcelable =====
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ===== Serializable =====
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== Enums =====
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===== Remove Logging (Production) =====
# Remove all Log.v, Log.d, Log.i calls (keep warnings and errors)
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ===== Optimization =====
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# ===== Warnings to Ignore =====
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===== Crash Reporting (if using Firebase Crashlytics) =====
# Uncomment when Crashlytics is integrated
# -keepattributes SourceFile,LineNumberTable
# -keep public class * extends java.lang.Exception

# ===== Security: Obfuscate Proprietary Logic =====
# Obfuscate everything not explicitly kept above
-repackageclasses 'o'
-allowaccessmodification
