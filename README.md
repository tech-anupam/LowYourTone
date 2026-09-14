# LowYourTone

**Offline voice-triggered actions for the moments when reaching a phone is hard.**

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 or newer">
  <img src="https://img.shields.io/badge/works-offline-171717?style=for-the-badge" alt="Works offline">
  <img src="https://img.shields.io/badge/on--device-speech-5B2EFF?style=for-the-badge" alt="On-device speech recognition">
  <img src="https://img.shields.io/github/license/tech-anupam/LowYourTone?style=for-the-badge&color=5B2EFF" alt="MIT license">
</p>

## Help launch LowYourTone on Google Play

<p align="center">
  <a href="upi://pay?pa=anupambuilds@fam&pn=Anupam&tn=LowYourTone%20Play%20Store%20launch&cu=INR">
    <img src="https://img.shields.io/badge/UPI-anupambuilds%40fam-5B2EFF?style=for-the-badge&logo=googlepay&logoColor=white" alt="Donate via UPI: anupambuilds@fam">
  </a>
  <img src="https://img.shields.io/badge/Play%20Store%20fund-%E2%82%B90%20%2F%20%E2%82%B92%2C500-FF6B6B?style=for-the-badge" alt="Play Store fund: ₹0 of ₹2,500">
</p>

Google Play developer registration costs **₹2,500**. The current fund is **₹0**. If you want this free safety tool to reach people beyond an APK download, contribute any amount to **`anupambuilds@fam`**. Every donation goes toward the Play Store launch.

---

## What it does

LowYourTone keeps a small offline listener active after you arm it. When it hears your chosen trigger phrase, it performs the action attached to that phrase.

| Trigger | Example action |
| --- | --- |
| `red mango` | Place an emergency call |
| `blue lantern` | Send an SOS location SMS |
| `quiet signal` | Start an audio recording |
| `night light` | Turn on the flashlight or alarm |

The app is intended for personal safety, accessibility, and hands-free shortcuts. It is not a replacement for local emergency services or a guaranteed emergency-response system.

## Safety-first trigger design

Accidental emergency calls are harmful, so the app does not treat any random sound as a trigger.

- **Exact phrase gate** — every word of the configured phrase must be heard in order; partial matches are rejected.
- **Two words for emergency calls** — emergency actions require a deliberately chosen two-word phrase. Avoid common words such as “help”, “call”, or “stop”.
- **Strict offline filtering** — keyword thresholds are biased toward fewer false positives.
- **30-second emergency lockout** — repeated recognition cannot place a series of emergency calls.

Choose an unusual, pronounceable phrase and test it around normal conversation, traffic, TV audio, and the places where it will be used.

## Locked-phone behavior

Once the app is configured, permissions are granted, and Listening is turned on, its foreground microphone service can continue listening while the screen is locked. A matching emergency phrase can request a call without the user opening the app or entering a PIN.

There are Android limits worth knowing:

- The user must complete setup and grant permissions **before** an emergency.
- After a complete reboot, newer Android versions may require the user to unlock and open the app once before microphone monitoring can resume.
- Battery restrictions can stop background work on some phones. Use the in-app **Battery → Fix** option and enable the manufacturer’s Autostart option when shown.
- A carrier, network outage, a device policy, or Android itself can still prevent a call from connecting. Always test on the intended phone and SIM.

## Setup in five minutes

1. Install the APK and open LowYourTone.
2. Allow **Microphone** and **Phone** permissions. Allow **SMS** and **Location** if an SOS location message is needed.
3. Create a unique two-word trigger and choose **Emergency Call** or another action.
4. Turn on **Listening** from the home screen.
5. Open Settings and confirm **EMERGENCY READY**. Disable battery optimisation when prompted.
6. Lock the phone and run several safe test triggers before depending on it.

For the Lock Screen action, enable **Screen Lock** in Settings once. Android keeps control of the PIN, pattern, or password; LowYourTone cannot read or bypass it.

## Available actions

| Category | Actions |
| --- | --- |
| Contact & alert | Call a contact, emergency call, SMS, SOS location SMS, WhatsApp message |
| Evidence & visibility | Audio recording, video capture prompt, flashlight, alarm, disco flash |
| Phone controls | Lock screen, silent mode, Do Not Disturb, maximum volume, stop media |
| Flexible shortcuts | Open an installed app or send a custom Android intent |

Different phrases can perform different actions. Non-emergency actions can be configured to ask for confirmation first.

## Privacy

- Speech recognition runs locally with PocketSphinx.
- The app does not request Internet permission and does not upload audio.
- Audio is only saved when the user explicitly configures the **Record Audio** action.
- Trigger history remains on the device.
- Source code is open for inspection.

## Download

<p align="center">
  <a href="https://github.com/tech-anupam/LowYourTone/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-5B2EFF?style=for-the-badge&logo=android&logoColor=white" alt="Download latest APK">
  </a>
</p>

Requires Android 8.0 (Oreo) or newer. Downloading from GitHub may require allowing installation from your browser or file manager.

## Built with

| Part | Technology |
| --- | --- |
| App | Kotlin, Jetpack Compose, Material 3 |
| Offline listener | PocketSphinx |
| App architecture | MVVM, Hilt, Room, DataStore |
| Location | Google Play Services Fused Location |
| Android range | min SDK 26 · target SDK 36 |

## Contribute

Bug reports, device-specific battery findings, trigger-testing notes, accessibility improvements, and pull requests are all useful.

1. Fork the repository.
2. Make a focused change on a branch.
3. Test it on a real device where possible.
4. Open a pull request with what you tested.

## License

[MIT](LICENSE)

<p align="center">
  Built in India for people who need a safer way to ask their phone for help.<br>
  <a href="https://github.com/tech-anupam">@tech-anupam</a>
</p>
