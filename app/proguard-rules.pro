# ============================================================================
# Beetle (BITL) — R8 / ProGuard rules
#
# Keep line numbers so crash reports from release builds are readable.
# ============================================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Annotations, generics and exceptions are load-bearing for reflection-based
# libraries below (kotlinx.serialization, JGit, Ktor).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions


# ============================================================================
# JNI — native PTY and LSP
#
# The C++ side resolves these by exact name via Java_<pkg>_<class>_<method>
# symbols. Renaming either the class or the method breaks the lookup at runtime
# with an UnsatisfiedLinkError that no build-time check would catch.
# ============================================================================
-keep class com.bsbeetle.bananaspaghetti.tenebrio.pty.Tenebrio { *; }
-keep class com.bsbeetle.bananaspaghetti.utils.languageHandling.LspServer { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}


# ============================================================================
# WebView JavaScript bridges
#
# Every editor / terminal / notebook / browser surface is a WebView talking to
# Kotlin through @JavascriptInterface. The JS calls these by name, so the names
# must survive. Keeping the annotated members alone is not enough — the bridge
# class itself must be kept too.
# ============================================================================
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.bsbeetle.bananaspaghetti.utils.sessionHandling.** { *; }
-keep class com.bsbeetle.bananaspaghetti.directGit.ui.DiffViewerSession { *; }
-keep class com.bsbeetle.bananaspaghetti.directGit.ui.MergeEditorSession { *; }
-keep class com.bsbeetle.bananaspaghetti.ui.browser.DevBridge { *; }


# ============================================================================
# kotlinx.serialization
#
# Serializers are generated as companion/synthetic members and looked up
# reflectively. These are the rules from the official README, plus a blanket
# keep for our own @Serializable models.
# ============================================================================
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.bsbeetle.bananaspaghetti.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.bsbeetle.bananaspaghetti.** { *; }


# ============================================================================
# Ktor (client OkHttp + server CIO/Netty)
#
# Ktor resolves engines through ServiceLoader and uses reflection across its
# plugin pipeline. Netty in particular fails loudly at runtime when shrunk.
# ============================================================================
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

-keep class io.netty.** { *; }
-dontwarn io.netty.**
# Netty probes for optional native/JVM-internal classes that don't exist on Android.
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn reactor.blockhound.**
-dontwarn com.oracle.svm.**
-dontwarn sun.security.**
-dontwarn java.lang.management.**


# ============================================================================
# SLF4J / logback
# ============================================================================
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**


# ============================================================================
# JGit + JSch (Git operations and SSH transport)
#
# JGit discovers transports, config parsers and pack formats via ServiceLoader
# and reflection. JSch resolves cipher/KEX implementations by class name string,
# so obfuscating them produces "Algorithm negotiation fail" at connect time.
# ============================================================================
-keep class org.eclipse.jgit.** { *; }
-dontwarn org.eclipse.jgit.**
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**


# ============================================================================
# NanoHTTPD (asset + live-preview servers)
# ============================================================================
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**


# ============================================================================
# Coil (image + SVG loading) / OkHttp
#
# OkHttp backs both Coil and the Ktor client engine. It ships its own consumer
# rules; the optional Conscrypt/BouncyCastle/OpenJSSE references it probes for
# are already covered by the -dontwarn lines in the Ktor/Netty block above.
# ============================================================================
-dontwarn coil.**
-dontwarn okhttp3.**
-dontwarn okio.**


# ============================================================================
# Compose / AndroidX
#
# R8 ships Compose rules, but Compose's own runtime keeps some members by
# reflection during composition tracing.
# ============================================================================
-dontwarn androidx.compose.**
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Tink backs EncryptedSharedPreferences and registers primitives by name.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**


# ============================================================================
# Enums referenced from serialized data keep valueOf/values.
# ============================================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ============================================================================
# Parcelables
# ============================================================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
