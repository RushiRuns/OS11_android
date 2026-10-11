# OS11 Android - ProGuard / R8 Rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}

# Room & SQLite Bundled
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.sqlite.driver.bundled.** { *; }
-keep class com.os11.app.data.local.entity.** { *; }
-keep class com.os11.app.data.local.dao.** { *; }
-keep class com.os11.app.data.local.converters.** { *; }
-keep class com.os11.domain.model.** { *; }
-keep class com.os11.domain.sync.** { *; }

# WorkManager & Hilt Workers
-keep class androidx.work.Worker
-keep class androidx.work.CoroutineWorker
-keep class androidx.work.ListenableWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.os11.app.worker.** { *; }

# Glance AppWidgets
-keep class androidx.glance.appwidget.** { *; }
-keep class com.os11.app.widget.** { *; }

# lib-recur (RFC 5545)
-keep class org.dmfs.rfc5545.** { *; }
-dontwarn org.eclipse.jdt.annotation.**

# Jsoup
-keep class org.jsoup.** { *; }

# JNI & System libs
-keepclasseswithmembernames class * {
    native <methods>;
}
