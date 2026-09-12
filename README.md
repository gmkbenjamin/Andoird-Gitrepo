# Andoird-Gitrepo (Gitrepo)

Git server over SSH for Android. Based on [Gidder](https://github.com/antoniy/Gidder).

This app runs a local SSH daemon on your phone so other machines on the same network can push and pull Git repositories. Users, repositories, and pull/push permissions are managed in the app UI.

> Repository name note: the GitHub repo is spelled **Andoird-Gitrepo** (typo retained). The app package/label is **Gitrepo** (`io.github.gmkbenjamin.gitrepo.beta`).

## Features

What the current `master` build actually provides:

- **SSH Git server** as a foreground service (Apache SSHD + JGit bare repos)
- **Users** with password and/or RSA public-key authentication (multiple keys separated by `-`)
- **Repositories** with per-user pull / pull+push permissions
- **Home screen** start/stop controls and current server address display
- **Wi‑Fi / Ethernet / hotspot detection** for connectivity status (device-dependent)
- **Optional DynDNS / No-IP** client to update a hostname with the device IP
- **Settings**: SSH port, status-bar notification, Wi‑Fi autostart/autostop, optional SSH shell, optional SCP
- **Encrypted repository backup** via Android Backup (experimental; requires an encryption password in Settings)
- **Home-screen widget** to toggle the SSH server
- **Email debug log** action in Settings

Modernisation already merged on `master`:

- Android Gradle Plugin **8.7.3** / Gradle **8.11.1**
- AndroidX AppCompat / Material / Preference (ActionBarSherlock removed)
- `compileSdk` / `targetSdk` **35**, `minSdk` **21**, Java **17**

## Requirements

| Item | Version / notes |
|------|-----------------|
| Android Studio | Recent stable (Ladybug / later recommended) |
| JDK | **17** (AGP 8 requires it) |
| Android SDK | Platform **35**, Build-Tools 34+ |
| Device / emulator | API **21+** |
| Network | Same LAN (or reachable IP) for Git clients |

## Build

From the repo root:

```bash
# Optional: point Gradle at your SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Open the project in Android Studio with **File → Open** on the repo root (Gradle sync should pick up the wrapper).

Other useful tasks:

```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest   # currently no unit tests in the tree
```

## Install and run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.github.gmkbenjamin.gitrepo.beta/.ui.activity.SplashScreenActivity
```

Or Run ▶ from Android Studio on a device/emulator.

### First-time use (app)

1. Open **Setup** and create at least one **user** and one **repository**.
2. Grant permissions / assign pull or pull+push as needed.
3. On **Home**, confirm network status, then **Start** the SSH server.
4. Note the address shown (typically `ip:port`, default port **2222**).

### First Git push from a client

`git clone` will not work on an empty bare repo before the first commit. Initialise locally, then push:

```bash
git init
git add .
git commit -m "initial commit"
git remote add origin ssh://USERNAME@DEVICE_HOST:2222/REPO_MAPPING.git
git push -u origin master
```

Replace `USERNAME`, `DEVICE_HOST`, and `REPO_MAPPING` with values from the app. On newer Git clients you may push `main` instead of `master` if that is your default branch name.

Password auth uses the password set for the user in the app. Public-key auth expects RSA OpenSSH public keys.

## Configuration

### Settings (in-app)

| Setting | Default | Purpose |
|---------|---------|---------|
| StatusBar notification | on | Ongoing notification while SSH is running |
| SSH server port | `2222` | Listen port for the SSH daemon |
| Autostart on Wi‑Fi ON | off | Start SSH when connectivity receiver sees Wi‑Fi ready |
| Autostop on Wi‑Fi OFF | off | Stop SSH when Wi‑Fi is not ready |
| SSH Shell | off | Allow interactive shell (`/system/bin/sh`); use client `-T` when enabled |
| SCP | off | Enable SCP command factory (summary notes `/sdcard` writability) |
| Repo backup | off | Encrypt/zip repos into Android Backup; set password when enabling |
| Email log | — | Compose email with debug/network info |

Start-on-boot and a custom repositories-directory preference exist in code/history but are **not** exposed in the current Settings UI. Repositories default to:

```text
/sdcard/gitrepo/repositories
```

(`WRITE_EXTERNAL_STORAGE` is only requested through API 28; on newer Android, shared-storage write access may fail.)

### Dynamic DNS

From Home → **Dynamic DNS**:

- Providers: **DynDNS.com** and **No-IP.com**
- Fields: domain, username, password, activate checkbox
- **Update** triggers an immediate provider update using the current device IP
- No-IP uses HTTP; DynDNS uses HTTPS. Cleartext HTTP is allowed via network security config for DynDNS/No-IP compatibility.

You need a real DynDNS/No-IP account for updates to succeed.

### SSH tips

- Keep **SSH Shell** disabled unless you intentionally want a device shell over SSH (security risk).
- Public-key auth currently compares RSA key moduli; ECDSA/Ed25519 are not supported by this stack path.
- Multiple public keys on a user: separate them with `-`.

## Project structure

Single-module Android app (`:app`):

```text
app/src/main/
  java/.../gitrepo/beta/
    app/           Application entry
    db/            ORMLite entities + DAOs
    git/           Bare-repo create/open/rename/delete (JGit)
    ssh/           SSH command factory / host keys / auth helpers
    dns/           DynDNS + No-IP update strategies
    service/       SSHDaemonService (foreground SSH server)
    receiver/      Connectivity, DynDNS, SSH toggle receivers
    ui/            Activities, fragments, preferences, widget
  res/             Layouts, drawables, preferences XML, themes
  AndroidManifest.xml
```

Tooling lives at the repo root (`build.gradle`, `settings.gradle`, `gradle/wrapper`).

## Known limitations / follow-ups

- **No automated tests** under `app/src/test` or `app/src/androidTest` yet; `testDebugUnitTest` succeeds with `NO-SOURCE`.
- **Scoped storage**: default repo path under `/sdcard/gitrepo/...` is fragile on Android 10+; may need app-specific storage migration.
- **Connectivity autostart** still relies on `CONNECTIVITY_ACTION` (restricted for background apps on modern Android); behaviour is more reliable while the app process is alive.
- **POST_NOTIFICATIONS** is declared; runtime permission prompting may still be needed on API 33+ for the status notification to appear.
- **Encrypted backup** depends on the Android Backup transport / device backup settings and size limits; treat as experimental.
- **Hotspot / Ethernet detection** is best-effort and OEM-dependent.
- **SSH stack age** (SSHD 0.14 / JGit 3.7) limits modern Git/SSH protocol and crypto options.
- Typo in the GitHub repository name (`Andoird`) is historical and intentional for this remote.

## License / provenance

Derived from Gidder. See upstream project history and this repository’s commits for attribution and changes.
