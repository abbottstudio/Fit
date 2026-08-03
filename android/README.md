# FitCoach Pro — Android app (Phase 1)

Implements Phase 1 from `../IMPLEMENTATION_STEPS.md`: manual check-in form,
submits to your backend's `POST /checkin`, shows the coach's reply, and
schedules a daily local reminder notification. No Health Connect, location,
FCM, or adaptive scheduling yet — those are Phase 2+.

**Revision 2:** a self-review pass against the first draft caught one
build-breaking bug and several real gaps, now fixed — see `../CHANGELOG.md`'s
latest entry for the full list. The most important one: the first draft
paired Kotlin 2.0.20 with the old `composeOptions.kotlinCompilerExtensionVersion`
scheme, which was retired in Kotlin 2.0 and would have failed Gradle sync
outright. Fixed by adding the `org.jetbrains.kotlin.plugin.compose` plugin.

## Important: this was written without a compiler

This sandbox has no Android SDK, no Gradle, and **no network access to
`dl.google.com` or Maven Central** (both return 403 — blocked by the sandbox's
network allowlist). That means none of this was Gradle-synced, compiled, or
run. It was hand-written to match the Android/Compose/Retrofit APIs as I
know them, cross-checked field-by-field against your actual backend
(`../backend/src/routes/checkin.js`), but **the first real verification
happens when you open this in Android Studio**. Two things to expect:

1. **Dependency versions may need a bump.** The versions pinned in
   `app/build.gradle.kts` (AGP 8.5.2, Kotlin 2.0.20, Compose BOM
   2024.09.00, Retrofit 2.11.0, etc.) were current and stable as of my
   training data, but I couldn't check what's actually latest today.
   Android Studio will flag anything outdated or incompatible on first
   sync and usually offers a one-click upgrade — accept it.
2. **The Gradle wrapper jar is missing.** `gradle/wrapper/gradle-wrapper.properties`
   is there (points at Gradle 8.7), but I couldn't download the actual
   `gradle-wrapper.jar` binary (same network restriction). Options:
   - Easiest: open the project in Android Studio — it detects the missing
     wrapper jar and offers to regenerate it automatically.
   - Manual: if you have Gradle installed locally, run `gradle wrapper --gradle-version 8.7`
     from this `android/` folder before opening in Studio.

If Studio's sync throws errors beyond these two categories, they're real
bugs in this code, not sandbox artifacts — worth telling me so I can fix them.

## Prerequisites

- Android Studio (latest stable) — bundles the JDK.
- Your backend (`../backend`) already running and reachable from your phone
  — see `../backend/README.md` and `../IMPLEMENTATION_STEPS.md` Phase 0 for
  the reachability decision (VPS, Cloudflare Tunnel, or same-wifi LAN for
  now). You need a URL and your `API_SHARED_SECRET` before check-in works.

## Opening and building

1. Android Studio → **Open** → select this `android/` folder.
2. Let Gradle sync run (see wrapper note above if it complains about the
   wrapper jar). Accept any suggested dependency-version bumps.
3. Connect your phone via USB with Developer Options + USB debugging
   enabled (`IMPLEMENTATION_STEPS.md` Prerequisites), or use an emulator for
   a first look — though Health Connect/location testing later needs a real
   device regardless.
4. Run ▶ to install a debug build directly, **or** build an installable APK:
   - `Build → Build App Bundle(s) / APK(s) → Build APK(s)` in Studio, or
   - `./gradlew assembleDebug` from a terminal (after the wrapper exists).
   - Output lands at `app/build/outputs/apk/debug/app-debug.apk`. Transfer
     that file to your phone and open it, or `adb install app-debug.apk`.
5. First launch → tap the ⚙ icon → enter your backend URL and
   `API_SHARED_SECRET` → **Save**. Nothing is hardcoded; this is stored
   locally on-device (see security note below).

## What's here

- `app/src/main/java/com/fitcoachpro/app/`
  - `MainActivity.kt` — requests the Android 13+ notification permission,
    hosts a 2-screen Compose nav graph (check-in ↔ settings).
  - `ui/CheckInScreen.kt` + `CheckInViewModel.kt` — the form (weight, sleep,
    energy/stress/motivation, soreness, joint pain, hydration, protein,
    steps, ready-to-train, optional freeform message), submits to the
    backend, displays the reply or an error.
  - `ui/SettingsScreen.kt` + `SettingsViewModel.kt` — backend URL, shared
    secret, reminder time/toggle.
  - `data/BackendApi.kt` + `BackendApiClient.kt` + `CheckInModels.kt` —
    Retrofit client and request/response models. **Field names match
    `../backend/src/routes/checkin.js` exactly** — if you change one side,
    change the other (same rule `AGENT_PROMPT.md` already applies to its own
    structured-output contract).
  - `data/PrefsRepository.kt` — DataStore-backed settings storage.
  - `notifications/` — `ReminderWorker.kt` (WorkManager job that fires the
    notification), `ReminderScheduler.kt` (schedules/cancels it, computes
    the next-fire delay), `NotificationHelper.kt` (channel + notification
    content, deep-links back into the check-in screen), `BootRescheduleReceiver.kt`
    (re-enqueues the reminder after reboot — belt-and-suspenders alongside
    WorkManager's own reboot persistence, since some OEM battery-optimization
    layers are known to interfere with it — see `IMPLEMENTATION_STEPS.md` Phase 7).

## Known limitations (by design, Phase 1 scope)

- **Reminder timing is approximate, not exact.** It's a WorkManager periodic
  job (~24h interval with a computed initial delay), which can drift by
  several minutes around your chosen time — that's WorkManager's
  battery-friendly design, not a bug. Exact-time delivery would need
  `AlarmManager.setExactAndAllowWhileIdle` plus the `SCHEDULE_EXACT_ALARM`
  permission flow, deliberately deferred past Phase 1.
- **No Health Connect, location, or FCM yet** — manual entry only. Phase 2+
  in `../IMPLEMENTATION_STEPS.md`.
- **Cleartext HTTP is allowed in debug builds only** (`network_security_config_debug.xml`),
  so you can point Settings at a bare `http://192.168.x.x:3000` backend while
  testing on the same wifi. Release builds require HTTPS — see
  `network_security_config.xml`. Once you have a real HTTPS backend URL
  (VPS + reverse proxy, or a tunnel service), use that in Settings and this
  restriction is moot.
- **The shared secret is stored in plain DataStore, not encrypted at rest.**
  Fine for a personal single-user app on your own phone; if you want to
  harden it later, that's a swap to `EncryptedSharedPreferences` or the
  Android Keystore — not done here to keep Phase 1 minimal.
- **Launcher icon is a placeholder** vector glyph, not real app branding —
  swap `app/src/main/res/drawable/ic_launcher_foreground.xml` whenever you
  want a real icon.

## Testing the loop end to end

1. Make sure `../backend` is running and reachable at the URL you put in
   Settings (`curl` it from your phone's browser first if unsure).
2. Fill the check-in form, hit Submit — you should get the coach's reply
   back and a new row in `daily_checkins` on the backend (see
   `../backend/README.md`'s verification steps).
3. Enable the reminder in Settings, set a time a couple minutes out, lock
   the phone, and confirm the notification arrives and tapping it opens the
   check-in screen.

## Getting an actual .apk without installing Android Studio

`../.github/workflows/build-apk.yml` builds this on GitHub's cloud runners
(which have normal internet access, unlike the sandbox this was written in)
and hands back a downloadable `.apk` as a build artifact — no local Android
Studio needed. It only runs once this project is in a GitHub repo, which has
to happen from your own machine — the sandbox that wrote this code hit a
hard limit trying to `git init` directly against this mounted drive (it
doesn't support the delete/rename operations git needs; see
`../CHANGELOG.md`'s eighth revision for what happened). From a terminal on
your machine, in the `fitcoach-pro` folder:

```bash
git init
git add -A
git commit -m "Initial commit: FitCoach Pro backend + Android app"
```

Then create a new empty repo on github.com (no README/license/gitignore —
you already have those), and:

```bash
git remote add origin https://github.com/<your-username>/<repo-name>.git
git branch -M main
git push -u origin main
```

The push triggers the workflow automatically. Go to the repo's **Actions**
tab, open the run, and download `fitcoach-pro-debug-apk` from the run's
**Artifacts** section once it finishes (a few minutes) — that zip contains
`app-debug.apk`, installable directly on your phone. You can also trigger a
build manually from the Actions tab without pushing new code, via
"Run workflow" (the `workflow_dispatch` trigger in the yml).

One thing to know: this backend and workout plan contain your personal
health data (weight, health metrics, the actual diet/exercise plan). If you
push to a **public** GitHub repo, that content becomes publicly readable.
Make the repo **private** unless you specifically want it public — GitHub
Actions and artifact downloads both work identically either way.

## Next steps

Phase 2 onward in `../IMPLEMENTATION_STEPS.md`: Health Connect sync,
schedule visibility + FCM push, the adaptive scheduler, location-based
activity detection, reporting/charts, hardening. Each is additive on top of
this Phase 1 skeleton, not a rewrite.
