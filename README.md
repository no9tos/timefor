# timefor

An Android app for a dopamine detox. Before you open an app you chose to limit, Timefor
asks how much time you want to spend in it. When that time runs out, the app is closed.
You can open it again, but you have to choose a duration again first.

## How it works

1. Open **Timefor**, tap **Enable in settings**, and turn on the *Timefor app limiter*
   accessibility service. Timefor uses it only to see which app is in the foreground.
2. In Timefor, switch on the apps you want to limit.
3. When you open a limited app, a full-screen question appears: *"How long do you want to
   spend in …?"*. Pick a preset (1, 5, 10, 15, 30 or 60 min) or any value from 1 to 180 minutes.
   - **Open for N min** starts a session and shows the app.
   - **Never mind** or **Back** takes you to the home screen.
4. One minute before the end, a toast warns you. When the time is up, Timefor sends you to the
   home screen and stops the app's process.
5. Opening the app again shows the question again.

A session keeps counting down if you switch away from the app. If you come back before the time
runs out, the app opens without asking.

## Project structure

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/timefor/app/AppMonitorService.kt` | Accessibility service. It detects when a limited app opens, shows the picker, and closes the app when the time is up. |
| `app/src/main/java/com/timefor/app/TimePickerActivity.kt` | The full-screen "how long?" question. |
| `app/src/main/java/com/timefor/app/MainActivity.kt` | Permission status and the list of apps to limit. |
| `app/src/main/java/com/timefor/app/LimitStore.kt` | Stores limited apps and session end times in SharedPreferences. |

## Install

Download the newest APK: https://github.com/no9tos/timefor/releases/download/latest/timefor.apk

Every push rebuilds it. Install it over the previous version; your settings are kept.

## Build

Requirements: JDK 17+ and the Android SDK (API 35). Open the project in Android Studio, or run:

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Min SDK 26 (Android 8.0), target SDK 35.

## Notes

- Android does not let a regular app force-close another app. When the time is up, Timefor goes
  to the home screen and then calls `killBackgroundProcesses`, so the app starts fresh next time.
- Some manufacturers (Xiaomi, Huawei, Samsung and others) stop background services aggressively.
  If the limiter stops working, turn off battery optimization for Timefor.
- Google Play has strict rules for apps that use the Accessibility API. You may need to
  explain this use when you publish the app.
