# Validation — 2026-11-06

- Native Java compiled and APK signature verified with Android SDK 35 tools.
- Upgrade installed on Android 15 emulator without deleting account data or the pinned widget.
- Real on-device TIM login captured an authenticated API response. No data JSON, Windows token or HAR was transferred into Android.
- Native background job manually triggered: HTTP 200 observed at 00:00:32 and 00:02:28. Snapshot showed 6.4 GB consumed / 28 GB total and 21.6 GB remaining.
- JobScheduler reports interval=1h, flex=5m, persisted.
- Existing widget resized to horizontal/full-width layout; screenshot: build/ash-widgets-final.png.
- App preview and widget use actual persisted TIM readings; no demo/preview fixture mode remains.
- Source detector: no findings (native sources are primarily verified on the emulator).
- Phone installation attempt rejected by POCO: INSTALL_FAILED_USER_RESTRICTED. Emulator is the validated target so far.
- Daily average requires 24 hours of this device's readings; it is not claimed as validated before that interval.
- Login refresh has a 90-second deadline and can still require manual reconnection if TIM changes its website or presents an additional challenge. Token-expiry behavior over multiple days has not yet been exercised.

## Version 0.3

- Removed widget footer, aligned numeric text and formatted total with one decimal.
- Widget tap opened MainActivity, confirmed via resumed activity in dumpsys.
- Android 15 emulator: changed Wi-Fi to mobile data and disabled both. Verified green and red ring screenshots with last TIM balance preserved. Restored both connections after testing. Foreground connection service confirmed active; the initial callback-only implementation froze in the background and was replaced with a service.
- Settings dropdown selected Smoky and reported successful application. Original Infinity_Rh GLSL compiled and rendered on emulator GPU; no shader errors in logcat. Screenshot: build/ash-settings-smoky.png.
- Final Smoky correction: original left-to-right alpha fade, black left side, original CSS opacity .86 and forward linear clock. Widget accepted 48 low-resolution background frames plus a sharp foreground without bitmap-budget errors. Final screenshot: build/ash-smoky-original.png. Theme persisted through APK upgrades.
- Opaque is the default. Transparent removes only the background. Theme preferences persist separately from account/session settings.

## Smoky color and loop refinement

- Native HSV picker displayed with saturation/value plane, hue strip, hex input, Cancel, Apply and Restore blue. It is visible in settings only for Smoky.
- Emulator UI selection applied a purple hue: both shader primary plume and Wi-Fi ring changed to the chosen color; the last TIM readings were preserved. Screenshot: build/ash-color-applied.png. Picker screenshot: build/ash-color-picker.png.
- Shader loop now crossfades eight tail/head frames and continues into the next forward-time sample, avoiding the hard boundary. Original GLSL remains unchanged. An immutable-bitmap blending failure found during validation was fixed by allocating mutable frames before compositing.
- APK rebuilt and installed successfully on emulator; native-source detector returned no findings.

## Version 0.4: tabs and Smoky precision

- Exercised all four bottom tabs using emulator UI automation. Home shows catalog/actions, Customização contains existing theme/picker controls, Login contains account/reconnect/form. Outros has exactly one content label, “Em desenvolvimento”, plus the shared navigation bar.
- Screenshots: build/ash-tab-home.png, ash-tab-custom.png, ash-tab-others.png and ash-tab-login.png.
- GPU precision query confirmed medium=10, high=23 on Android Emulator OpenGL ES Translator (NVIDIA RTX 5060). Promoted the shader float default to highp at runtime; screenshots show smooth noise detail without the coarse dark quantization of the reported image. Physical-phone rendering is not yet visually verified; its screen was off during the diagnostic capture.

## Version 0.5: floating icon dock

- Built and installed versionCode=5 / versionName=0.5 on the emulator, confirmed from PackageManager after installation.
- Replaced the full-width text bar with a floating dock, consistent outline icons, selected capsule and ripple feedback. Scroll content reserves space above the dock. Exercised all four destinations; Outros still has only its development label.
- Captured build/ash-dock-home.png, ash-dock-custom.png, ash-dock-others.png and ash-dock-login.png. Visual inspection confirmed complete labels and no dock overlap on the tested display.
- Smoky now requires high precision instead of falling back to the visibly quantized mediump variant. GPU log confirmed high=23, medium=10 for the installed 0.5 build. Both preview and pinned widget show fine smoke detail in emulator captures. Physical-phone rendering remains unverified.
- Copied final APK into the physical phone's /sdcard/Download/ash-widgets.apk. SHA-256 on Windows and phone matched: 0d735aa7b3509971ff98c0741bb51797353a2f22c2ca76d18449be57f1ec69c7. This copies the installer; it does not claim installation on the phone.
- Native-source detector returned no findings.


## Versions 0.6-0.7: live shader, themes, picker

- The original LRan 21st.dev hovered shader replaced the Infinity_Rh variant; resource equations are retained verbatim, with runtime highp/hover/compositing adaptation. See SMOKY.md.
- Live EGL query: highp=23, RGBA=8/8/8/8. On the NVIDIA hardware backend, a 3316-frame live run averaged 59.7 Hz on the emulator's 60 Hz display. The app requests the highest supported display mode; physical POCO frame rate remains unverified.
- Theme UI exercised all four choices: Opaque, Transparent, Opaque Smoky, Transparent Smoky. Each reported successful application. Existing Smoky preferences migrate to Opaque Smoky.
- Fixed live viewport size mismatch using actual EGL surface dimensions; direct screenshot build/fixed7.png confirms full-card smoke in Transparent Smoky. No small bottom-left rectangle remains.
- Darkened translucent glass treatment for Transparent and Transparent Smoky in the final build, following user feedback. This simulates frosted glass and does not claim wallpaper backdrop blur.
- Themed HSV dialog displayed and Apply exercised. Screenshot build/picker-styled.png. Navy surface, swatch/hex field, Restore blue and 52 dp Cancel/Apply actions.
- Widget cache increased to 240 160x71 frames at a requested 16 ms cadence. Foreground text/ring remain 720x320. Android accepted the widget update without bitmap-budget or shader errors. Actual launcher delivery is not guaranteed to match display Hz.
- Background HTTP 200 observed in the prior 0.6 run with the launcher foreground; expired authentication also renewed without visiting the app Home tab. Hourly scheduling remains unchanged.
- Windows emulator host GPU output showed coarse dark quantization even in the wallpaper; the direct Android screencap did not. Software backend removed that display defect but ran live shader slowly. Treat host display quality separately from APK rendering; physical phone visuals remain unverified.
- Final native sources compiled and signed; source detector returned no findings. Final APK copied to physical phone /sdcard/Download/ash-widgets.apk. SHA-256 matched Windows: d9824fdb9ccc17b665885ac84419adc309a4aa1e3bcc10698b5fd3d6bb4d5ef8. This is installer delivery, not phone installation.

- Final corner correction: launcher clips frames with its native rounded outline (14 dp) instead of baking the corner mask into 160x71 animation frames; the app retains the original 30/720 relative radius. Final APK installed successfully on emulator.


## Version 0.8: launcher icon and crisp corners

- Added an authored adaptive vector icon: white A, pale blue widget tile and usage ring on navy. Includes a monochrome layer for themed launchers. Manifest icon/roundIcon point to the adaptive resource. Icon visually verified in emulator dock (build/corners8.png).
- Moved the rounded outline to each animated ImageView, because the host overrides the root widget outline. Image height adjusts to the cached image aspect ratio, centered against the sharp foreground. Rounded corners are clipped at display resolution instead of encoded in low-resolution animation pixels. Screenshot: build/corners-final8.png.
- Version 0.8 installed on emulator, compiled/signature verified, no AndroidRuntime/AppWidgetHostView errors during the visual check. Latest installer copied to phone Downloads and SHA-256 matched: 97527ea5ca8eca3bb4a74822d7b98fa5102cc6938cc2d2c81ded3ed7bc105054.


## Version 0.9: in-app preview swipe

- Home and Customization use the shared two-page WidgetPreview pager. TIM foreground and centered Em desenvolvimento transition independently of the background: easeOutQuart (1-(1-t)^4), 120 ms horizontal slide and 160 ms alpha, both directions.
- Selection survives data/theme refresh; animations settle on activity hide/detach, and reduced-motion mode switches pages without animation. Horizontal gesture interception leaves vertical scrolling with the parent. Keyboard arrows and accessibility forward/back actions are available.
- Compiled, signature verified and installed on emulator. Native-source detector returned no findings. User confirmed the preview swipe works while testing; automated UI-idle verification was interrupted by navigation away to the launcher, so no claim of a full automated gesture pass is made.
- Copied installer to phone Downloads; SHA-256 matched: a70251959d8793aa553b6ed66a827b693970132e440f62d23cd69645cac533fd.
- This changes in-app previews only. Clarification pending whether remaining requested surface is Home inside the app or the separately hosted Android launcher widget.

## Version 0.13: preview-only custom vertical swipe

- Restored custom vertical paging in Home and Customization previews: easeOutQuart, 120 ms translation and 160 ms alpha, with stationary background.
- Fixed ImageView opacity overload: floating-point alpha values preserve drawable opacity.
- Built, signature verified and installed on the emulator. Direct screenshots confirmed the development label and TIM content after forward/reverse gestures.
- Removed launcher paging; the home-screen widget renders one TIM card.
- Copied the APK to the connected phone Downloads and verified matching SHA-256. Installation and rendering on the physical phone remain unverified.
