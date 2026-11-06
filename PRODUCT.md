# ash-widgets
## Platform
android, personal POCO X7 Pro, Portuguese.
## Purpose
Show real TIM mobile data usage on the home screen, independently of the PC.
## Approved direction
Horizontal 4×2 widget matching the user's supplied references: thin blue ring and animated signal bars on the left, divider and usage figures on the right, translucent dark panel. Show used/total GB and remaining GB with one decimal. No average/update footer. Tap opens the app. Data is fetched by the APK, every hour, without PC fixtures or services.
## Stack
Native Android Java; delegated by approval of the native APK plan. TIM authentication through the official login page; session remains local. No credentials embedded in the APK.
## Constraints
Unknown data stays explicitly unavailable. Android may delay periodic jobs. Initial account setup happens on the device. Credentials and session are encrypted with Android Keystore. A catalog supports adding future widget types; existing component identifiers remain stable for upgrades.
