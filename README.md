# Techno TV Launcher

A free, ad-free, futuristic home screen for **Google TV / Android TV**, with **NOVA**, a talking
assistant, a cinematic start-up screen and a 3D blue glow that follows your remote.

![Techno TV](store/graphics/tv-banner-1280x720.png)

## Features

- **Home screen**: clock, weather for the city you choose, Featured app cards, an Apps panel with an
  All-apps page, and an Inputs panel (TV, HDMI, AV, USB).
- **NOVA**, a scripted assistant (not a real AI) with an arc-reactor avatar:
  - comments when you rest on an app, and before switching inputs;
  - suggests something after 15 s of no button presses (OK on the card opens it);
  - hold OK on the card to mute it.

  Lines play from recordings in `voice/recorded/`, or are spoken by the TV's own text-to-speech.
- **Start-up screen** on power-on: the reactor spins up, "TECHNO TV", "All systems online", and
  a spoken welcome. With a Bluetooth speaker it waits (up to 5 s) for the speaker to reconnect.
- **Quick Settings and Cast & Connect** shortcuts.
- **Private**: only the Internet permission; no accounts, ads or analytics ([privacy policy](PRIVACY.md)).

## Install

- **Google Play**: coming soon (see [PLAY_RELEASE.md](PLAY_RELEASE.md) for the publishing steps).
- **From a computer**: open this folder in [Claude Code](https://claude.com/claude-code) and say
  *"set up the launcher on my TV"* (see [CLAUDE.md](CLAUDE.md)), or by hand:
  ```bash
  ./install.sh <tv-ip>        # find it with ./find_tv.sh
  ```
  Needs JDK 17, Android SDK platform 36 + build-tools 36, and adb. No Gradle needed.

After installing, the app explains the Home-button permission and opens *Settings → Accessibility*
so you can turn on **Techno TV**. That's what lets the Home button open it. You can turn it off
any time.

## Make it yours

| What | Where |
|---|---|
| Weather city | Asked on first launch; select the weather card to change it |
| Featured apps | `Config.java` → `FEATURED` |
| NOVA's lines | `voice/lines.txt`; recordings in `voice/recorded/<key>.mp3`, then `./make_voice.sh` |
| Wallpaper / avatar | `assets/wallpaper.jpg`, `assets/arc_reactor.png` |

## Build for Google Play

```bash
./bundle.sh          # -> release/techno-tv.aab, signed with your upload key
```

Then follow [PLAY_RELEASE.md](PLAY_RELEASE.md). The store listing text and graphics are in [store/](store/).

## How it works (and its limits)

- Google TV doesn't let apps replace its home screen. With the accessibility service on, Techno
  TV opens itself whenever the Google TV home appears, so you may see the Google TV home for a
  split second when pressing Home.
- The real boot animation can't be changed without rooting; the start-up screen plays right after it.
- Never disable the Google TV home app (`com.google.android.apps.tv.launcherx`): Google's setup
  app then loops on Home.

## Uninstall

*Settings → Apps → Techno TV → Uninstall*, or `./uninstall.sh <tv-ip>`.

## License

[MIT](LICENSE). Credits in [CREDITS.md](CREDITS.md).
