# proguard-rules.pro

# ============ إعدادات عامة ============
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-renamesourcefileattribute SourceFile

# ============ Kotlin ============
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-dontwarn kotlin.**

# ============ Coroutines ============
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============ AndroidX / ViewBinding ============
-keep class * extends androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# ============ ViewModel / LiveData ============
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# ============ Models (مهم عشان ما تنكسر بيانات الصفحات) ============
-keep class com.pdfmangaeditor.models.** { *; }

# ============ PhotoView ============
-keep class com.github.chrisbanes.photoview.** { *; }
-dontwarn com.github.chrisbanes.photoview.**

# ============ Coil ============
-keep class coil.** { *; }
-dontwarn coil.**
-keepclassmembers class * {
    @coil.annotation.ExperimentalCoilApi *;
}

# ============ PDF (android.graphics.pdf) ============
# مكتبة نظام، ما تحتاج قواعد خاصة، لكن نحافظ على أي كلاسات مخصصة تتعامل معها
-keep class com.pdfmangaeditor.pdf.** { *; }

# ============ Parcelable (لو استخدمنا @Parcelize لاحقًا) ============
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============ تحذيرات عامة نتجاهلها ============
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**