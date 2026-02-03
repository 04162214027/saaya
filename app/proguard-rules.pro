# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }

# Keep database models
-keep class com.saaya.automator.data.** { *; }

# Keep accessibility service
-keep class com.saaya.automator.core.SaayaService { *; }
