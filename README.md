# LowYourTone

**When you can't reach your phone, your voice becomes your lifeline.**

<p align="center">
  <img src="https://img.shields.io/github/v/tag/tech-anupam/LowYourTone?label=release&style=for-the-badge&color=8A2BE2" alt="Latest Release">
  <img src="https://img.shields.io/github/downloads/tech-anupam/LowYourTone/total?style=for-the-badge&color=8A2BE2" alt="Downloads">
  <img src="https://img.shields.io/github/license/tech-anupam/LowYourTone?style=for-the-badge&color=8A2BE2" alt="License">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-8A2BE2?style=for-the-badge" alt="Platform">
</p>

---

## The problem

India registered **4,41,534 crimes against women in 2024**. That is 1,210 cases every single day, and only the ones that were reported. In 96.8% of rape cases the attacker was already known to the victim. A 2025 survey found 40% of urban Indian women still consider their surroundings unsafe.

In a real emergency the standard advice is: call the police. That assumes you can reach your phone, unlock it, open an app, and make a call in seconds of panic. Most people cannot.

LowYourTone removes every one of those steps. **Say a secret word. Your phone acts.**

---

## What it does

You pick a word or short phrase. You pick what your phone should do when it hears that phrase. The app listens in the background, always, with no internet and no cloud.

| Say your phrase | Your phone does |
|---|---|
| your secret word | calls your emergency contact |
| your secret word | sends your GPS location via SMS |
| your secret word | records audio silently |
| your secret word | turns the torch on and flashes it |
| your secret word | plays a loud alarm |
| your secret word | sends a WhatsApp message |

You choose the phrase. You choose the action. Nobody else needs to know.

Pick something that sounds ordinary in conversation - a random two-word phrase in any language. It will not attract attention. But your phone will know.

---

## Why it works offline

This is not Google Assistant or speech-to-text. It uses **PocketSphinx**, a speech recognition engine that runs entirely on the device. No audio ever leaves your phone. No servers. No accounts. No internet needed.

```
1. Set a secret phrase (e.g. "mango rain")
2. Choose an action (e.g. call Mom and SMS my location)
3. Turn on Listening
4. Say "mango rain" from anywhere - phone does it instantly
```

---

## Safety-first trigger design

Accidental emergency calls are harmful. The app does not treat any random sound as a trigger.

- **Exact phrase gate** - every word of the configured phrase must be heard in sequence. Partial matches are rejected.
- **Two words for emergency calls** - emergency actions require a deliberately chosen two-word phrase. Avoid common words like "help", "call", or "stop".
- **Strict offline filtering** - keyword thresholds are tuned toward fewer false positives.
- **30-second emergency lockout** - repeated recognition cannot place a series of emergency calls back to back.

Choose an unusual, pronounceable phrase and test it around normal conversation, traffic, TV audio, and the places you plan to use it.

---

## Locked phone behavior

Once the app is configured, permissions are granted, and Listening is on, the foreground microphone service continues listening while the screen is locked. A matching emergency phrase can trigger a call without unlocking or opening the app.

A few things worth knowing:

- Permissions must be granted before an emergency. Setup takes about five minutes.
- After a full reboot, some Android versions require one unlock before microphone monitoring resumes.
- Battery restrictions on Xiaomi, OPPO, Vivo, and Huawei can stop background work. Use the in-app Battery Fix option and enable the manufacturer Autostart setting when shown.
- A carrier outage, network problem, or Android system policy can still prevent a call from connecting. Always test on the exact phone and SIM you plan to use.

---

## Setup in five minutes

1. Install the APK and open LowYourTone
2. Allow Microphone and Phone permissions. Allow SMS and Location if you want location messages.
3. Create a unique two-word phrase and choose an action
4. Turn on Listening from the home screen
5. Go to Settings and confirm EMERGENCY READY. Disable battery optimisation when prompted.
6. Lock the phone and run a few safe test triggers before depending on it

For the Lock Screen action, enable Screen Lock in Settings once. Android keeps full control of your PIN or password. LowYourTone cannot read or bypass it.

---

## Available actions

| Category | Actions |
|---|---|
| Emergency | Emergency call, call a contact, SOS location SMS |
| Message | Send SMS, WhatsApp message |
| Alert | Sound alarm, flashlight, disco flash |
| Record | Audio recording, video capture |
| Phone controls | Lock screen, silent mode, Do Not Disturb, max volume, stop media |
| Shortcuts | Open an installed app, custom Android intent |

Different phrases can trigger different actions. Non-emergency actions can ask for confirmation before running.

---

## Installing from GitHub

> **Play Protect may flag the APK as unreviewed.** This happens with any APK that has not gone through the Play Store review. The app contains no malware - the full source code is open for inspection in this repository.

To install:

1. Download the APK from the [latest release](https://github.com/tech-anupam/LowYourTone/releases/latest)
2. Open the APK from your Downloads folder or notification
3. If Android asks, tap **Install anyway** or **More details > Install anyway**
4. If Play Protect blocks it, tap **Got it** on the warning and then **Install**
5. You can re-enable Play Protect scanning afterward from Play Store > Profile > Play Protect

The app requests only the permissions it needs and uses no internet connection.

---

## Privacy

- No internet permission - the app literally cannot send data anywhere
- No analytics, no tracking, no telemetry
- All speech recognition runs on-device using PocketSphinx
- Audio is never stored unless you explicitly set up the Record Audio action
- Trigger history stays on the device
- Source code is open for inspection

---

## Play Store?

Publishing on the Google Play Store costs **2,500 rupees** as a one-time developer registration fee. If this app is useful to you or you think it should reach more people:

**UPI: `anupambuilds@fam`**

Every rupee goes directly toward the Play Store listing. If we hit the goal, the app goes live on the Play Store - free, forever, for everyone.

---

## Built with

| Part | Technology |
|---|---|
| App | Kotlin, Jetpack Compose, Material 3 |
| Offline listener | PocketSphinx |
| Architecture | MVVM, Hilt, Room, DataStore |
| Location | Google Play Services Fused Location |
| Android range | min SDK 26, target SDK 36 |

---

## Contribute

Bug reports, device-specific battery findings, trigger-testing notes, accessibility improvements, and pull requests are all useful.

1. Fork the repository
2. Make a focused change on a branch
3. Test on a real device where possible
4. Open a pull request with what you tested

---

## Research references

| Source | Finding |
|---|---|
| NCRB "Crime in India 2024" (May 2026) | 4,41,534 registered cases; 64.6 per lakh rate; 96.8% of rape cases involved a known perpetrator |
| Ashoka University / NCRB analysis | 1,210 cases per day on average; domestic violence as leading category |
| National Commission for Women | 25% of complaints are domestic violence; rest include stalking, assault, dowry harassment |
| 2025 Urban Safety Survey | 40% of urban Indian women consider their surroundings unsafe |
| NDTV / The Hindu (2024-2025) | High-profile cases in Bengaluru and Delhi involving broad-daylight assault and stalking |
| Government initiatives | 112 emergency number, Nirbhaya Fund, One Stop Centres, 181 women helpline |

---

## License

Open source under the [MIT License](LICENSE).

---

<p align="center">Built in India. For India. By someone who got tired of just being angry.</p>
<p align="center"><a href="https://github.com/tech-anupam">@tech-anupam</a></p>
