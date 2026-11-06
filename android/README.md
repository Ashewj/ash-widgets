# ash-widgets - Android

Build from the repository root: `powershell -ExecutionPolicy Bypass -File build-android.ps1`.
APK: `android/build/ash-widgets.apk`. Existing package/component names are retained for upgrades and pinned widgets.

Native Android Java app with four tabs and a floating icon dock. Internet TIM is the first catalog widget. The device signs in through the official TIM website and queries native HTTPS with its own encrypted session. Credentials and tokens are stored locally through Android Keystore; no PC data or embedded credentials are used. Expired authentication triggers a login retry; website changes or additional challenges can require manual reconnection.

JobScheduler requests hourly updates, with a five-minute flexible window, persisted across reboot. Android can delay jobs. Tap the widget to open the app, then use Atualizar agora for a manual request. Errors retain the last valid balance. A connection foreground service keeps the installed widget's network status current: Wi-Fi uses the selected accent, mobile data green, offline red. The average/refresh footer has been removed.

Themes: Opaque, Transparent, Opaque Smoky and Transparent Smoky. The latter two expose the themed HSV/hex picker. App previews use a display-paced live shader; launcher widgets use cached background frames with a requested 16 ms cadence, subject to launcher scheduling. See SMOKY.md for source attribution, blending and limitations.

Emulator: WidgetPoco, Android 15/API 35, WHPX. SDK and generated artifacts are in tools and android/build. A phone APK copy is an installer delivery, not proof of installation or visual validation on the POCO.
