# ADAPT Mobile Deployment Guide

This document explains how to produce production-ready Android artifacts.

## 1) Build Release Artifacts

From `Adapt_mobile_app/`:

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

Generated files:
- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

## 2) Configure Production API URL

Set the API base URL at build time:

Windows:

```bat
gradlew.bat bundleRelease -PADAPT_API_BASE_URL=https://api.your-domain.com/api/
```

macOS/Linux:

```bash
./gradlew bundleRelease -PADAPT_API_BASE_URL=https://api.your-domain.com/api/
```

## 3) Configure Signing (Recommended)

Release signing is optional in code, but required for distribution.
Provide these Gradle properties (in `~/.gradle/gradle.properties` or CI secrets):

- `ADAPT_RELEASE_STORE_FILE`
- `ADAPT_RELEASE_STORE_PASSWORD`
- `ADAPT_RELEASE_KEY_ALIAS`
- `ADAPT_RELEASE_KEY_PASSWORD`

Example:

```properties
ADAPT_RELEASE_STORE_FILE=C:/keys/adapt-release.jks
ADAPT_RELEASE_STORE_PASSWORD=***
ADAPT_RELEASE_KEY_ALIAS=adapt
ADAPT_RELEASE_KEY_PASSWORD=***
```

When these properties are present, release builds are automatically signed.

Validate signing configuration explicitly:

Windows:

```bat
gradlew.bat verifyReleaseSigning
```

macOS/Linux:

```bash
./gradlew verifyReleaseSigning
```

Run full signed release readiness in one command:

Windows:

```bat
gradlew.bat checkReleaseReadiness
```

macOS/Linux:

```bash
./gradlew checkReleaseReadiness
```

## 4) Smoke Test Checklist Before Upload

- App launches to login screen.
- Login/register succeeds against target backend.
- All tabs load: Status, Logs, Tasks, Profile.
- Tasks tab can open routines and complete steps.
- Assist mode opens current routine and can launch guided task.
- Release build succeeds without code changes.

## 5) Play Store Upload

Use the generated `.aab` file (`app-release.aab`) for Play Console uploads.

## 6) CI Signed Release Readiness

The repository workflow `.github/workflows/release-hardening.yml` can run signed release checks automatically.

Required repository secrets:

- `ADAPT_RELEASE_STORE_FILE_BASE64` (base64-encoded keystore file)
- `ADAPT_RELEASE_STORE_PASSWORD`
- `ADAPT_RELEASE_KEY_ALIAS`
- `ADAPT_RELEASE_KEY_PASSWORD`

When these are present, the workflow runs `checkReleaseReadiness` and fails fast on signing misconfiguration.
