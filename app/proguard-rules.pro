# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==============================================================================
# MediaPipe GenAI API (LiteRT LM) ProGuard Rules
# ==============================================================================

# MediaPipe GenAI クラスを保持
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keep interface com.google.mediapipe.tasks.genai.** { *; }

# MediaPipe フレームワーク
-keep class com.google.mediapipe.framework.** { *; }
-keep interface com.google.mediapipe.framework.** { *; }

# TensorFlow Lite（MediaPipeが内部で使用）
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ネイティブメソッドを保持
-keepclassmembers class * {
    native <methods>;
}

# Kotlinコルーチン
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose用（念のため）
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# アプリ固有のデータクラスを保持（Parcellable等に必要）
-keepclassmembers class com.example.gemmabench.** {
    public <init>(...);
}

# Enum クラスを保持
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}