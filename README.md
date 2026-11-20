<div align="center">

<img src="docs/images/banner.svg" width="100%" alt="OneBuds: Realme Buds T310 battery and noise control">

<br><br>

[![Latest release](https://img.shields.io/github/v/release/dcoder-me/onebuds?label=version&color=4A5D8F)](../../releases/latest)
[![Downloads](https://img.shields.io/github/downloads/dcoder-me/onebuds/total?color=4A5D8F)](../../releases)
![Android 14+](https://img.shields.io/badge/Android-14%2B-3DDC84?logo=android&logoColor=white)
![No internet permission](https://img.shields.io/badge/internet-not%20used-16201D)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)

### [⬇ Download the APK](../../releases/latest)

| Light | Dark | In action |
| :---: | :---: | :---: |
| <img src="docs/images/screenshot-light.png" width="240" alt="Battery and noise control, light mode"> | <img src="docs/images/screenshot-dark.png" width="240" alt="Battery and noise control, dark mode"> | <img src="docs/images/demo.gif" width="240" alt="Demo: buds connect and the noise mode switches"> |

</div>

OneBuds shows your earbuds' battery and switches noise modes without opening the bulky official app. It wakes up on its own when the buds connect and goes quiet when they leave.

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>🔋 Battery at a glance</h3>
      <p>Left, right and case, with charging status.</p>
    </td>
    <td width="50%" valign="top">
      <h3>📌 Live Update</h3>
      <p>Battery pinned to the top of the notification shade with a status-bar pill (Android 16).</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>🎧 Noise control</h3>
      <p>Switch Noise cancelling, Off or Transparency from the app, the notification or a widget.</p>
    </td>
    <td width="50%" valign="top">
      <h3>🧩 Home screen widgets</h3>
      <p>Battery, noise control, or both, from 1×1 to 4×2.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>⚡ Quick Settings tile</h3>
      <p>One tap from the top of the screen. Hide the app icon if you only use the tile and widgets.</p>
    </td>
    <td width="50%" valign="top">
      <h3>🔒 No internet permission</h3>
      <p>The app talks only to your buds over Bluetooth. Nothing leaves your phone.</p>
    </td>
  </tr>
</table>

## ⚙️ How it works

```mermaid
flowchart LR
    A["Buds come in range"] --> B["Android wakes OneBuds"]
    B --> C["Read battery<br/>Fast Pair + control channel"]
    C --> D["Notification · widget · tile"]
```

The buds speak a private Bluetooth protocol. OneBuds reads battery from the buds' Fast Pair broadcast and from their control channel, and sends noise-mode commands on the same channel. Android's companion-device feature wakes the app when the buds come in range, so nothing runs in the background while they're away.

Protocol notes: [docs/protocol/realme-link-tl-protocol.md](docs/protocol/realme-link-tl-protocol.md)

## 📥 Download

Get the latest APK from the **[Releases page](../../releases/latest)**, copy it to your phone and install it (allow "Install unknown apps" when asked).

## 📋 Requirements

- **Android 14 or newer** (it uses the companion-device presence feature added in Android 14)
- **Realme Buds T310** (other Realme buds on the same protocol may work, but are untested)

## 🚀 Setup

1. Pair the buds in your phone's Bluetooth settings as usual.
2. Open OneBuds → **Allow** the Bluetooth and notification permissions.
3. Tap **Detect buds**, then **Link** your buds.
4. Done. Battery appears whenever the buds connect.

Optional: add the **Realme Buds** Quick Settings tile and a home screen widget.

## 🧑‍💻 Build from source

<details>
<summary><b>Build steps</b></summary>

```bash
./gradlew assembleDebug     # debug APK in app/build/outputs/apk/debug/
./gradlew assembleRelease   # signed only if keystore.properties exists
./gradlew test              # unit tests
```

No buds at hand? Debug builds can fill in sample buds for screenshots:

```bash
adb shell am broadcast -n com.example.budsbattery/.DemoDataReceiver --es noise TRANSPARENCY
```

Release signing reads `keystore.properties` from the project root (git-ignored):

```properties
storeFile=/path/to/your.jks
storePassword=…
keyAlias=…
keyPassword=…
```

</details>

## 🏷️ Versions

Each release is a git tag (`v1.1.0`, `v1.2.0`, …) with its APK attached on the [Releases page](../../releases). The app's version lives in `app/build.gradle.kts` (`versionName`, `versionCode`).

## 🧱 Built with

Kotlin · Jetpack Compose · Material 3 · Glance widgets · DataStore · Coroutines

---

Not affiliated with Realme. "Realme" is a trademark of its owner.
