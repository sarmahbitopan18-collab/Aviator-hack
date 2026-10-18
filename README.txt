Aviator Live Screen Reader — Android Studio source project

What it does:
- Requests Android's explicit MediaProjection permission when you tap Start screen reading.
- Uses on-device Google ML Kit text recognition to look for visible multiplier strings like 1.23x.
- Keeps a short local list of detected results and supports manual entry.
- Does not connect to wonderhindi.com, intercept hidden game data, place bets, or predict future results.

Build:
1. Open this folder in Android Studio (Giraffe or newer recommended).
2. Allow Gradle sync and install the Android SDK platform 35 if prompted.
3. Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the debug APK on your own Android phone.
5. Tap Start screen reading and approve Android's screen-capture prompt.
6. Return to the game; the capture indicator will be visible. Stop capture from the app when done.

Notes:
- This is source code, not a prebuilt APK. I cannot compile it here because Android SDK/Gradle build environment is not available.
- Android versions and OEM policies may restrict screen capture, and some apps can show black frames when protected.
- OCR may mistake numbers or detect old UI labels; verify values. The screen reader is for reading already visible results, not guaranteed prediction.
- Use only where allowed by the game and local rules. No auto-betting.
