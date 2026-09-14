# LowYourTone

**When you can't reach your phone, your voice becomes your lifeline.**

<p align="center">
  <img src="https://img.shields.io/github/v/tag/tech-anupam/LowYourTone?label=release&style=for-the-badge&color=8A2BE2" alt="Latest Release">
  <img src="https://img.shields.io/github/downloads/tech-anupam/LowYourTone/total?style=for-the-badge&color=8A2BE2" alt="Downloads">
  <img src="https://img.shields.io/github/license/tech-anupam/LowYourTone?style=for-the-badge&color=8A2BE2" alt="License">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-8A2BE2?style=for-the-badge" alt="Platform">
</p>

---

## The Problem We Can't Ignore

India reported **4,41,534 cases** of crimes against women in 2024 alone.

That's **1,210 women every single day.**

And these are just the ones that got reported.

In **96.8% of rape cases**, the attacker was someone the victim already knew. In cities like Delhi and Bengaluru, women are harassed in broad daylight - during morning commutes, on college campuses, walking home from work. A 2025 study found that **40% of urban Indian women** still feel unsafe in their own neighborhoods.

The worst part? In a real emergency, **you can't always reach your phone.** Your hands might be held. Your phone might be in your bag. You might be driving. You might just be frozen with fear.

Every safety app out there needs you to unlock your phone, open the app, find a button, and press it. In a real crisis, none of that works.

**We asked a simple question: What if your voice was enough?**

---

## What LowYourTone Does

LowYourTone listens for a secret word that only you know. Say it, and your phone acts - immediately, automatically, silently.

No buttons to press. No screen to unlock. No internet required. **Just your voice.**

| Say This | Your Phone Does This |
|----------|---------------------|
| *your secret word* | Calls your emergency contact |
| *your secret word* | Sends your GPS location via SMS |
| *your secret word* | Starts recording audio as evidence |
| *your secret word* | Turns on flashlight as a signal |
| *your secret word* | Triggers a loud alarm |
| *your secret word* | Sends a WhatsApp message |

You choose the word. You choose the action. Nobody else knows.

**Your wake word could be anything** - a normal-sounding word that won't alert an attacker. Something like "weather" or "homework" or a word in your own language. It sounds innocent. But your phone knows what it means.

---

## Why This Exists

After the 2012 Nirbhaya case, India got stricter laws. After every trending case since - Hathras, Hyderabad, Bengaluru, Kolkata - we got outrage, candlelight marches, hashtags. The news cycle moves on. The problem doesn't.

The government launched the 112 emergency number, the Nirbhaya Fund, One Stop Centres. These matter. But they all assume one thing: **that the victim can make a call.**

What if she can't?

What if her hands are pinned? What if she's being watched? What if her phone is in her pocket and she can't take it out?

LowYourTone was built for that exact moment. The moment where everything else fails, and the only thing left is your voice.

---

## How It Works

```
1. You set a secret wake word (e.g., "pineapple")
2. You assign an action (e.g., send location SMS to Mom)
3. LowYourTone listens in the background - always, silently
4. You say "pineapple" - your phone sends the SMS instantly
```

**Everything happens on your phone. Nothing goes to the cloud. No internet needed. No audio ever leaves your device.**

This isn't speech-to-text or Google Assistant. This uses PocketSphinx - an offline speech recognition engine that runs entirely on your device. Your privacy is absolute.

---

## Features

- **100% Offline** - Works without WiFi, mobile data, or any internet connection
- **Background Listening** - Works even when your phone is locked or in your pocket
- **17 Actions** - Call, SMS, location share, flashlight, alarm, audio record, WhatsApp, and more
- **Multiple Wake Words** - Different words for different emergencies
- **Zero Cloud** - No audio data ever leaves your phone
- **Customizable Sensitivity** - Adjust how easily your wake word is detected
- **Action History** - See every time a wake word was triggered
- **Battery Optimized** - Designed to run for hours without draining your battery
- **Works on Xiaomi/Redmi** - Special handling for aggressive battery management

---

## Not Just for Women

While women's safety was the original motivation, LowYourTone is for everyone:

- **Elderly parents** who can't navigate phone screens during a fall
- **Delivery workers** who need hands-free emergency calls while riding
- **Children** who need a simple way to alert parents
- **Anyone with disabilities** who can't easily use touchscreens
- **Solo travelers** who want a silent panic system
- **Night shift workers** walking to their vehicle alone

---

## Download

<p align="center">
  <a href="https://github.com/tech-anupam/LowYourTone/releases/latest">
    <img src="https://img.shields.io/badge/Download%20APK-Latest%20Release-8A2BE2?style=for-the-badge&logo=android" alt="Download APK">
  </a>
</p>

> **Requires Android 8.0 (Oreo) or higher.** Download the APK from the latest GitHub release and install it manually.

---

## Play Store?

We want to put this on the Google Play Store so it reaches the people who need it most - women in tier-2 and tier-3 cities, college students, night shift workers.

But publishing on the Play Store costs **₹2,500** (Google's one-time developer registration fee).

If this app helped you, or if you think it should reach more people:

**UPI: `anupambuilds@fam`**

Every rupee goes directly toward the Play Store listing and keeping this project alive. If we hit the goal, the app goes live on the Play Store - free, forever, for everyone.

---

## Privacy

- **No internet permission** - The app literally cannot send data anywhere
- **No analytics, no tracking, no telemetry**
- **All speech recognition happens on-device** using PocketSphinx
- **Audio is never stored** unless you explicitly use the "Record Audio" action
- **Open source** - Read every line of code yourself

---

## Tech Stack

For developers who want to contribute or understand how it works:

| Component | Technology |
|-----------|------------|
| Language | Kotlin 100% |
| UI | Jetpack Compose + Material 3 |
| Speech Engine | PocketSphinx (offline, on-device) |
| Architecture | MVVM + Hilt DI |
| Database | Room |
| Preferences | DataStore |
| Location | Google Play Services FusedLocation |
| Build | Gradle + AGP 9.1.0 |
| Target SDK | 36 (Android 16) |
| Min SDK | 26 (Android 8.0) |

---

## Contributing

This is a solo project built by one person. If you're a developer and want to help:

1. Fork the repo
2. Create a feature branch
3. Make your changes
4. Open a pull request

No contribution is too small. Even fixing a typo helps.

---

## Research & References

The statistics and context mentioned in this README come from publicly available sources:

| Source | What It Says |
|--------|-------------|
| **NCRB "Crime in India 2024"** (May 2026) | 4,41,534 registered cases of crimes against women in 2024; 64.6 per lakh crime rate; 96.8% of rape cases involved known perpetrators |
| **Ashoka University / NCRB Analysis** | 1,210 cases registered per day on average; domestic violence as the leading category |
| **National Commission for Women (NCW)** | 25% of complaints are domestic violence; rest include stalking, assault, and dowry harassment |
| **IndiaSpend / SPRF** | Significant underreporting due to social stigma and lack of faith in the justice system |
| **2025 Urban Safety Survey** | 40% of women in urban India consider their surroundings unsafe; harassment peaks during 5 AM-8 PM |
| **NDTV / The Hindu (2024-2025)** | Multiple high-profile cases in Bengaluru and Delhi involving broad-daylight assault and stalking |
| **Government Initiatives** | 112 emergency number, Nirbhaya Fund, One Stop Centres (OSCs), 181 women's helpline |

---

## License

This project is open source under the [MIT License](LICENSE).

---

<p align="center">
  <b>Built in India. For India. By someone who got tired of just being angry.</b>
</p>

<p align="center">
  <a href="https://github.com/tech-anupam">@tech-anupam</a>
</p>
