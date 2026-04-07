# ADAPT Mobile App Local Setup Guide

This guide helps a teammate run the Android app from a fresh clone.

## 1) Prerequisites

- Android Studio (recent stable version)
- JDK 17 (configured in Android Studio)
- Android SDK and required build tools (installed via SDK Manager)
- Optional: Android device with USB debugging enabled

## 2) Open The Project In Android Studio

From Android Studio:
- File -> Open
- Select the folder: Adapt_mobile_app
- Wait for Gradle sync to complete

## 3) Check SDK And JDK Settings

In Android Studio:
- File -> Settings -> Build, Execution, Deployment -> Gradle
- Set Gradle JDK to Java 17

If local.properties is missing or incorrect, set Android SDK path.
This file is machine-specific and should not be edited for teammates unless needed locally.

## 4) Run On Emulator Or Device

- Start an emulator from Device Manager, or connect a physical device
- Select app run configuration
- Click Run

## 5) CLI Build (Optional)

From repository root:

Windows Command Prompt (cmd):

```bat
cd Adapt_mobile_app
```

macOS/Linux Terminal:

```bash
cd Adapt_mobile_app
```

Windows:

```bat
gradlew.bat assembleDebug
```

macOS/Linux:

```bash
./gradlew assembleDebug
```

### Build with a custom backend URL

Windows:

```bat
gradlew.bat assembleDebug -PADAPT_API_BASE_URL=http://10.0.2.2:3001/api/
```

macOS/Linux:

```bash
./gradlew assembleDebug -PADAPT_API_BASE_URL=http://10.0.2.2:3001/api/
```

### Generate release artifacts

Windows:

```bat
gradlew.bat assembleRelease
gradlew.bat bundleRelease
```

macOS/Linux:

```bash
./gradlew assembleRelease
./gradlew bundleRelease
```

Output paths:
- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/bundle/release/app-release.aab`

## 6) Tests (Optional)

Windows:

```bat
gradlew.bat testDebugUnitTest
```

macOS/Linux:

```bash
./gradlew testDebugUnitTest
```

## 7) Common Issues

- Gradle sync fails: verify internet access and Gradle JDK is Java 17
- SDK not found: fix SDK path in local.properties via Android Studio setup
- Device not listed: restart adb, reconnect device, or reboot emulator
- Build cache issues: run Clean Project, then Rebuild Project

## 8) Team Workflow Notes

- Do not commit local machine paths or credentials.
- Keep local.properties as local-only configuration.
- Configure signing only via Gradle properties/environment, never in source control.
