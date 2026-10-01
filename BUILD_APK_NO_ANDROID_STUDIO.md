# Build an installable APK without Android Studio

The source project can be built entirely in GitHub Actions. This is useful if you only want an APK to install on your Samsung phone.

## One-time setup

1. Create a free GitHub account if you do not already have one.
2. Create a new **private** repository, for example `fasting-coach`.
3. Upload the entire contents of this project folder to the repository. Make sure the hidden `.github` folder is included.
4. Open the repository's **Actions** tab.
5. Open the workflow named **Build Android APK**.
6. Choose **Run workflow** and run it on the main branch.
7. When the workflow finishes, open the completed run.
8. Under **Artifacts**, download **FastingCoach-debug-apk**.
9. Unzip the downloaded artifact. It contains `FastingCoach-debug.apk`.

## Install it on a Samsung phone

1. Transfer `FastingCoach-debug.apk` to the Samsung phone, or download it there from GitHub.
2. Tap the APK in **My Files** or the Downloads notification.
3. If Android blocks the install, tap **Settings** when prompted and allow **Install unknown apps** for the app you used to open the APK (for example Chrome or My Files).
4. Return to the installer and tap **Install**.
5. Open **Fasting Coach**.

## What works in the debug APK

- Fasting timer and fasting history.
- Profile and calorie/macronutrient target calculation.
- Manual/demo meal flow and automatic daily totals.
- Weekly report calculations.
- Health Connect permission flow and step-reading code, where supported and granted.

## Food photo scanning on a physical phone

The current Android project points to `http://10.0.2.2:8000`, which is only the Android emulator's route to a backend running on the development computer. A real Samsung phone cannot use that address.

For food-photo AI scanning on a real phone, deploy the included `backend/` service to an HTTPS host, then replace `API_BASE_URL` in `app/build.gradle.kts` with that public HTTPS URL before running the APK build again.

Never place an OpenAI API key directly in the Android application.

## Debug-build note

This workflow creates a debug-signed APK intended for personal testing. Android will allow installation after you approve the unknown-app source. A public Play Store release should use a proper release signing key, privacy policy, Health Connect declarations, production backend, and the normal Google Play release process.
