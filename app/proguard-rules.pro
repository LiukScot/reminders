# Room, Compose, DataStore and OkHttp ship their own consumer rules — nothing to repeat here.

# Reflected by name from AndroidManifest/AlarmManager PendingIntents, so R8 can't see the use.
-keep class com.liukscot.reminders.notifications.** { *; }

# okhttp reaches for these optional platform providers only when present; without the rules R8
# warns about the missing classes and fails the build.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (via security-crypto) references errorprone's compile-only annotations at class level.
-dontwarn com.google.errorprone.annotations.**
