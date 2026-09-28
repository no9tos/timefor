# Timefor

**A dopamine-detox app for Android.** Before you open an app you chose to limit, Timefor asks how
much time you want to spend in it. When the time is up, the app closes. You can open it again,
but you have to choose a duration again first.

[**⬇ Download the latest APK**](https://github.com/no9tos/timefor/releases/latest/download/timefor.apk)
· [All releases](https://github.com/no9tos/timefor/releases)
· [Русский](#русский)

## Features

- Asks **"How long do you want to spend in …?"** before a limited app opens: 1, 5, 10, 15, 30 or
  60 minutes, or any value up to 180.
- Warns one minute before the end. When the time is up, returns to the home screen and closes
  the app.
- The next launch asks again, so every visit is a conscious choice.
- A searchable list of your apps to choose which ones to limit.
- English and Russian interface. Android 8.0 or newer.

## Privacy

- Timefor has **no internet permission**. Nothing leaves your phone.
- It uses the Accessibility service **only to see which app is in the foreground**. It does not
  read, record or store what is on your screen.
- Your settings are stored only on your device.
- The code is open. You can check all of this yourself.

## Install

1. Download [`timefor.apk`](https://github.com/no9tos/timefor/releases/latest/download/timefor.apk)
   on your phone and open it. Allow installs from your browser if Android asks.
   If Play Protect warns about an unknown app, choose **More details → Install anyway**.
2. Open Timefor, tap **Enable in settings** and turn on the **Timefor app limiter** service.
   - On Android 13 and newer, if the switch is greyed out ("Restricted setting"), open
     **Settings → Apps → Timefor → ⋮ → Allow restricted settings** first.
3. Choose the apps to limit.

Updates install over the previous version, and your settings are kept.

On Xiaomi, Huawei, Samsung and some other phones, set Timefor's battery usage to **Unrestricted**
so the system does not stop it in the background.

## How it works

| File | Purpose |
| --- | --- |
| [`AppMonitorService.kt`](app/src/main/java/com/timefor/app/AppMonitorService.kt) | Accessibility service. It detects when a limited app opens, shows the question, and closes the app when the time is up. |
| [`TimePickerActivity.kt`](app/src/main/java/com/timefor/app/TimePickerActivity.kt) | The full-screen "how long?" question. |
| [`MainActivity.kt`](app/src/main/java/com/timefor/app/MainActivity.kt) | Permission status and the list of apps to limit. |
| [`LimitStore.kt`](app/src/main/java/com/timefor/app/LimitStore.kt) | Stores limited apps and session end times on the device. |

Android does not let a regular app force-close another one. When the time is up, Timefor goes to
the home screen and then stops the app's background process, so it starts fresh next time.

## Build from source

Requirements: JDK 17+ and the Android SDK (API 35). Open the project in Android Studio, or run:

```sh
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Every push is built by GitHub Actions. Releases are described in [RELEASING.md](RELEASING.md),
and changes are listed in [CHANGELOG.md](CHANGELOG.md).

## Contributing

Bug reports and pull requests are welcome. Please include your phone model and Android version
when reporting a problem.

## License

[GPL-3.0](LICENSE). You may use, study, change and share Timefor. Modified versions you
distribute must also be open source under the same license.

---

## Русский

**Приложение для дофаминового детокса на Android.** Перед тем как открыть выбранное приложение,
Timefor спрашивает, сколько времени вы хотите в нём провести. Когда время выходит, приложение
закрывается. Открыть его снова можно, но сначала придётся снова выбрать время.

[**⬇ Скачать последнюю версию (APK)**](https://github.com/no9tos/timefor/releases/latest/download/timefor.apk)

**Возможности**
- Перед открытием спрашивает: «Сколько времени вы хотите провести в …?» Можно выбрать 1, 5, 10,
  15, 30 или 60 минут или любое значение до 180.
- За минуту до конца предупреждает. Когда время вышло, возвращает на главный экран и закрывает
  приложение.
- При следующем открытии спрашивает снова.

**Приватность.** У Timefor нет доступа к интернету, данные не покидают телефон. «Специальные
возможности» нужны только чтобы видеть, какое приложение открыто. Содержимое экрана не читается
и не сохраняется.

**Установка**
1. Скачайте [`timefor.apk`](https://github.com/no9tos/timefor/releases/latest/download/timefor.apk)
   на телефон и откройте. Если Play Защита предупредит, нажмите **Подробнее → Всё равно
   установить**.
2. Откройте Timefor → **Включить в настройках** → включите службу **Timefor — ограничение
   приложений**.
   - На Android 13 и новее, если переключатель серый: **Настройки → Приложения → Timefor → ⋮ →
     Разрешить ограниченные настройки**.
3. Отметьте приложения, которые нужно ограничивать.

На Xiaomi, Huawei, Samsung поставьте для Timefor режим батареи **Без ограничений**.

**Лицензия:** [GPL-3.0](LICENSE). Код можно свободно использовать, изучать и изменять, но
изменённые версии тоже должны оставаться открытыми.
