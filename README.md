# LowYourTone

Offline voice wake word automation for Android. Say a word, trigger an action. No internet required, no audio ever leaves your phone.

<p align="center">
  <img src="https://img.shields.io/github/v/release/tech-anupam/LowYourTone?style=for-the-badge&color=8A2BE2" alt="Latest Release">
  <img src="https://img.shields.io/github/downloads/tech-anupam/LowYourTone/total?style=for-the-badge&color=8A2BE2" alt="Downloads">
  <img src="https://img.shields.io/github/license/tech-anupam/LowYourTone?style=for-the-badge&color=8A2BE2" alt="License">
  <img src="https://img.shields.io/badge/platform-Android%208.0%2B-8A2BE2?style=for-the-badge" alt="Platform">
</p>


## What this app actually does

Think of it like a personal panic button and remote control, except the remote is your own voice, and it works without signal, without WiFi, and without any company listening in.

You pick a word. You pick an action. From then on, saying that word out loud makes your phone do that action, instantly, even if the phone is in your pocket and the screen is off.

Some examples people use it for:

- Say "help now" and your phone calls emergency services and texts your location to a chosen contact, automatically.
- Say a specific name and your phone calls that person directly, no unlocking, no tapping.
- Say "record this" and your phone quietly starts recording audio in the background.
- Say "lights on" and your flashlight turns on.

All of this happens fully on the device. LowYourTone never sends your voice, your location, or anything else to a server. There is no server. Everything is processed and stored locally on your phone.

## Why it exists

Phones are supposed to help in a crisis, but in a real crisis, most people cannot unlock their phone, find the right app, find the right contact, and tap call, all in a few seconds of panic. LowYourTone removes every one of those steps. You just speak.

It is built for situations like:

- Personal safety, especially for people walking alone, commuting late, or in an unsafe situation.
- Hands-busy moments, cooking, driving, working, where reaching for the phone is not an option.
- Elderly or vulnerable users who need a simpler way to reach help than navigating a phone screen.
- Anyone who just wants a faster way to trigger common phone actions by voice, without relying on Google Assistant, Siri, or any cloud-based voice assistant.

## How to download and install

Do not clone this repository and try to build it unless you are a developer. Regular users should download the ready-to-use app instead.

1. Go to the **Releases** tab of this repository: [LowYourTone Releases](https://github.com/tech-anupam/LowYourTone/releases)
2. Open the latest release at the top of the page.
3. Under **Assets**, download the file ending in `.apk`.
4. Open the downloaded file on your Android phone. If Android warns you about installing from outside the Play Store, allow it for this file. This is normal for apps distributed directly on GitHub instead of the Play Store.
5. Open the app, grant the permissions it asks for, and set up your first wake word.

The app requires **Android 8.0 or newer**.

## Permissions, explained plainly

The app will ask for a few permissions during setup. Here is what each one is actually for, in plain terms:

| Permission it asks for | Why it needs it |
|---|---|
| Microphone | To listen for your wake word. This is the core of the app. |
| Phone calls | So a wake word can actually place a call, like calling emergency services or a contact. |
| SMS | So a wake word can send a text message, including sharing your location in an emergency. |
| Contacts | So you can pick who gets called or texted, instead of typing numbers manually. |
| Location | So an emergency wake word can send your current location to someone. |
| Camera | Only used to control the flashlight, the app does not take photos or video through this permission unless you set up a record action yourself. |
| Notifications | To show you that the listening service is active and running in the background. |

None of these permissions are used to collect data or send anything off your phone. They exist purely so the app can carry out the actions you configure.

## What you can set a wake word to do

Every wake word you create can be linked to one of the following actions:

- Call a specific contact
- Emergency SOS call, with location sent automatically
- Send a text message
- Send your live location by text
- Open any app on your phone
- Turn the flashlight on or off
- Flash the light rapidly with vibration, useful for getting attention
- Record an audio memo
- Record a video
- Play a loud alarm sound
- Send a pre-written WhatsApp message
- Switch your phone to silent
- Turn Do Not Disturb on or off
- Set every volume to maximum
- Stop any recording, sound, or flashlight instantly
- Lock your screen
- Trigger a custom action, for advanced users who want to connect it to other tools

## Why it works without internet

Most voice assistants send your voice to a company's server to figure out what you said. LowYourTone does not do this. It uses an offline speech recognition engine that runs entirely on your phone's hardware. This means:

- It works in airplane mode.
- It works with no SIM card.
- It works in places with zero signal.
- Nobody, including the app's own developer, can ever access what you said.

This is by design. A safety tool that depends on an internet connection is not a safety tool you can rely on in the moments that matter most.

## A note before you rely on this for emergencies

This app is built carefully and tested, but it is an independent project, not a certified emergency service product. Please test your wake words and actions yourself after setup, keep your phone charged, and do not treat this as a guaranteed replacement for calling emergency services directly when you are able to.

## Feedback and issues

Found a bug, or have an idea for a new action or feature? Open an issue on this repository. Feedback directly shapes what gets built next.

---

<p align="center">Built by <a href="https://github.com/tech-anupam">Anupam</a></p>
