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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ------------------------------基本指令区---------------------------------
#指定压缩级别
-optimizationpasses 5
#混淆时采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#打印混淆的详细信息
-verbose
#关闭优化
-dontoptimize
# 保持注解
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型, 这在JSON实体映射时非常重要
-keepattributes Signature
# 屏蔽警告
-ignorewarnings
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
#混淆时不使用大小写混合，混淆后的类名为小写(大小写混淆容易导致class文件相互覆盖）
-dontusemixedcaseclassnames

# -------------------------------基本指令区--------------------------------