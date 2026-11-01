# PkgX

A simple package manager for Android built on [Shevery-API](https://github.com/HmnDev-Tech/Shevery-API). It lists the installed apps, lets you search them, and runs common package operations through the privileged Shevery server (ADB shell or root).

## Features

- Browse and search installed apps, with an option to show system apps
- Force stop (`am force-stop`)
- Clear app data (`pm clear`)
- Freeze / unfreeze (`pm disable-user` / `pm enable`)
- Uninstall for the current user, optionally keeping data (`pm uninstall --user 0 [-k]`)
- Open the system app info screen
- Confirmation dialogs for destructive actions, with an extra warning for system apps
- Built-in protection: PkgX refuses to modify itself and the Shevery manager

Commands run through `SheveryPrivilege.newProcess(...)` as an argument array, so nothing is interpreted by a shell and no hidden-API stubs are needed.

## Requirements

- Android 7.0 (API 24) or newer
- [Shevery](https://github.com/HmnDev-Tech/shevery) installed and running (root, ADB, or wireless debugging)

## Usage

1. Start the Shevery server.
2. Open PkgX and tap **Request permission**, then approve the prompt from Shevery.
3. Tap an app in the list and pick an action.

## Building

PkgX includes the `aidl`, `shared`, `api` and `provider` modules of Shevery-API as source modules, so the Shevery-API repository must be present in a `Shevery-API/` folder at the project root.

### GitHub Actions

Push the project to a GitHub repository. The **Build APK** workflow checks out Shevery-API itself, builds a debug APK, and uploads it as the `PkgX-debug` artifact. To make builds reproducible, replace `ref: main` in `.github/workflows/build.yml` with a specific commit SHA.

### Local build

Requires JDK 21 and the Android SDK (platform 36, build-tools 36.0.0).

```bash
git clone https://github.com/HmnDev-Tech/Shevery-API
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

The plugin versions in `settings.gradle` (Android Gradle Plugin 9.4.0-alpha07) and the Gradle wrapper match the ones used by Shevery-API. If Shevery-API updates its toolchain, update them to match.

## Project structure

```
app/                      PkgX application (Java, Material 3)
  src/main/java/com/haydareiss/pkgx/
    MainActivity.java     UI, connection state, actions
    PrivilegedShell.java  runs commands through the Shevery server
    AppAdapter.java       app list adapter
    AppEntry.java         list item model
Shevery-API/              cloned separately (not tracked)
```

## Credits

- [Shevery](https://github.com/HmnDev-Tech/shevery) and [Shevery-API](https://github.com/HmnDev-Tech/Shevery-API) by HmnDev-Tech
- Based on the [Shizuku](https://github.com/RikkaApps/Shizuku) API design by RikkaApps
