# Daily Reminder  (Version 1.0.0)
© 2026 Shri Swami Samarth · Developed by Swapnil Palande

## How to get the APK (about 5 minutes)
1. Install Android Studio (free): https://developer.android.com/studio
2. Unzip this folder. In Android Studio choose File > Open and select the "DailyReminder" folder.
3. Wait for "Gradle sync" to finish (needs internet the first time).
4. Test on your phone: connect it by USB (turn on USB debugging) and press the green Run button.
5. Make the APK file: Build > Build App Bundle(s) / APK(s) > Build APK(s).
   The file is at app/build/outputs/apk/debug/app-debug.apk. Copy it to your phone and install it.
   (For Play Store, use Build > Generate Signed Bundle / APK.)

## What is inside
- Tabs: Routine, Water, Medicine, Meetings, Notes (bottom bar)
- Menu (three dots, top right): Profile, Office time, Backup, About us
- Alarm: Beep Beep twice, repeats every 8 seconds until Done or Snooze (stops by itself after 2 minutes)
- Alarm screen opens even when the phone is locked; alarms are set again after phone restart
- Hourglass app icon (res/drawable/ic_launcher_foreground.xml, color in res/values/colors.xml)

## Please read
- Allow Notifications when the app asks. On some phones (Xiaomi, Oppo, Vivo, Samsung) also set
  Battery > Daily Reminder > "Unrestricted", or alarms can be delayed.
- Spelling underlines, suggestions and auto-correct in Notes come from your phone keyboard and spell checker.
  Android has no built-in sentence/grammar checker, so grammar checking is not in version 1.0.0.
- Automatic nightly backup is not in version 1.0.0. Use Backup now / Share backup / Import.
- App name "Daily Reminder" and package name com.swapnil.dailyreminder are placeholders. Change them in
  res/values/strings.xml and app/build.gradle.kts if you want.
- Not yet tested on a real phone. If Android Studio shows an error, copy it and send it to me.
