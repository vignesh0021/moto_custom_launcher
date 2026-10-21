# ==============================================================================
# MOTOROLA EDGE 50 LAUNCHER - AGGRESSIVE R8 FULL MODE PROGUARD RULES
# Optimizes for < 45 MB Resting PSS & Sub-6.94ms Execution Latency
# ==============================================================================

# Run maximum aggressive optimization passes
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.motorola.launcher.opt'

# Remove unused attributes to minimize DEX size & classloader overhead
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Strip all android.util.Log calls in release builds (zero allocation/string overhead)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}

# Room SQLite FTS5 Entity Preservation
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}

# Jetpack Compose 1.12 Compiler & Runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-dontwarn androidx.compose.**

# Baseline Profile & ProfileInstaller
-keep class androidx.profileinstaller.** { *; }

# Fast Immutable Collections
-keep class kotlinx.collections.immutable.** { *; }

# Coroutines optimizations
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Hardware Icon Pipeline & Binary Cache Structs
-keepclassmembers class com.motorola.launcher.data.icon.BinaryDiskCache$* {
    <fields>;
}
