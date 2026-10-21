# Publishing Techno TV on Google Play: step by step

## 0. Build the upload bundle
```bash
./bundle.sh
```
This makes `release/techno-tv.aab`, signed with your **upload key** in `keystore/upload.jks`
(created on first run; its password is in `keystore/upload-password.txt`).
**Back up the whole `keystore/` folder.** Without it you can't publish updates.

## 1. Developer account
- Sign up at https://play.google.com/console ($25 one-time). Choose a **personal** or
  **organization** account (organizations need a D-U-N-S number but skip the tester rule below).
- Verify your identity when asked.

## 2. Create the app
Play Console → **Create app** → name "Techno TV: Launcher & Home", app, free.

## 3. Closed test (required for new personal accounts)
New personal developer accounts must run a **closed test with at least 12 testers for 14 days
in a row** before they can publish to everyone.
- **Testing → Closed testing → Create track**, upload `release/techno-tv.aab`.
- Add 12+ testers' Google emails (friends and family with a Google TV), share the opt-in link,
  and ask them to keep it installed for 14 days.
- Then **Apply for production access**.

## 4. App content (Policy → App content)
- **Privacy policy**: public URL of `PRIVACY.md` (or the same text on a web page).
- **Ads**: no ads.
- **App access**: all functions available without special access.
- **Content rating**: questionnaire → Utility / Productivity; no violence etc.
- **Target audience**: 13+ (not designed for children).
- **Data safety**:
  - Data collected: **Approximate location** (purpose: app functionality = weather), not shared,
    not stored on servers, processed on device / via ipapi.co. Everything else: none.
  - Data encrypted in transit: yes (HTTPS). Users can request deletion: not applicable (no account).
- **Accessibility API declaration** (asked because of the accessibility service): say the app is
  a launcher and the service is used to open Techno TV when the user presses Home and to show the
  start-up screen on power-on; it does not read screen content or collect data; the user is
  shown a disclosure and must turn it on themselves. Google asks for a **short video**: record the
  disclosure dialog appearing, choosing "Turn on", enabling it in Accessibility, then pressing Home.

## 5. Store listing
Copy from `store/STORE_LISTING.md` and upload the images from `store/graphics/` plus at least one
1920×1080 TV screenshot.

## 6. Android TV
**Release → Advanced settings → Form factors → Android TV → Opt in.** Google reviews TV apps
separately against its TV quality guidelines (D-pad navigation, banner, no touchscreen needed).
Techno TV already declares `LEANBACK_LAUNCHER`, a 320×180 banner, and no touchscreen requirement.

## 7. Production
After the closed test: **Production → Create release**, upload the same or a newer `.aab`
(increase the version: `VERSION_CODE=2 VERSION_NAME=1.0.1 ./bundle.sh`), roll out.

## Honest note on review
The accessibility service is the part most likely to be questioned. If Google rejects it, the
app still works as a normal app you open from the home screen; the Home-button takeover and the
power-on screen would then be dropped for the Play version.
