# IMPLEMENTATION_STEPS.md — FitCoach Pro

Concrete build checklist. Each phase matches `CLAUDE.md`'s phased plan, but with actual setup steps and a verification checkpoint before you move on. One gap the design doc didn't resolve: **how the backend is reachable from your phone off home wifi** — addressed in Phase 0 below, since it affects everything after it.

---

## Prerequisites

- [ ] Android Studio (latest stable) — bundles the JDK you need.
- [ ] Node.js LTS + npm.
- [ ] A physical Android phone (not just an emulator) — Health Connect and background location behave inconsistently on emulators; you'll want real-device testing from Phase 2 onward regardless.
- [ ] Anthropic API key from console.anthropic.com.
- [ ] Enable Developer Options + USB debugging on your phone (Settings → About Phone → tap Build Number 7 times, then Settings → Developer Options).

## Phase 0 — Setup

- [ ] Create the project structure: `fitcoach-pro/backend` and `fitcoach-pro/android`.
- [ ] **Resolve backend reachability now, not later**: pick one —
  - Cheapest/simplest for a solo project: a small VPS (~$5/mo, e.g. DigitalOcean/Linode/Render) — backend runs there permanently, phone talks to it over the internet like any API.
  - Zero ongoing cost: run the backend on a home machine and expose it via **Cloudflare Tunnel** (free) — works but depends on that machine staying on.
  - Do not try to run the backend only on localhost — it won't be reachable once your phone is off your home wifi, which defeats the point of alerts working anywhere.
- [ ] Backend: `npm init -y`, then `npm install express better-sqlite3 @anthropic-ai/sdk dotenv cors node-cron`.
- [ ] Create `.env` with `ANTHROPIC_API_KEY=...` and a `API_SHARED_SECRET=...` (random string) — **the app must send this as a bearer token on every request**. Without this, anyone who finds your backend's URL can run up your Claude API bill. This is not optional given the backend is internet-facing.
- [ ] Create the SQLite schema from `CLAUDE.md`'s data model section (`config`, `workout_plan`, `daily_checkins`, `workout_logs`, `nutrition_logs`, `activity_logs`, `schedule`, `notifications_log`, `weekly_reports`, `monthly_reports`).
- [ ] Transcribe the attached workout plan PDF into the `workout_plan` table — do this by hand; it's a one-time job and far more reliable than writing a PDF parser for a single document.
- [ ] Android: File → New Project → Empty Activity (Compose). Package name your choice (e.g. `com.<you>.fitcoachpro`). **minSdk 28** (reliable Health Connect support).
- [ ] Add to `build.gradle`: Health Connect client (`androidx.health.connect:connect-client`), Retrofit + OkHttp (backend calls), WorkManager, Compose Navigation.

**Checkpoint:** backend starts locally (`node index.js`) and responds to a health-check route; Android project builds and runs an empty screen on your phone.

## Phase 1 — Core loop: manual entry + local notifications

- [ ] Backend: `POST /checkin` — accepts the daily check-in fields, injects recent history + `AGENT_PROMPT.md` as the Claude system prompt, calls the Messages API, parses the trailing JSON block (see `AGENT_PROMPT.md`'s structured-output contract), writes to `daily_checkins`, returns the human-readable coaching response.
- [ ] Backend: require the `API_SHARED_SECRET` bearer token on this and every other route.
- [ ] Android: Compose screen with the check-in form (weight, sleep, energy/stress/motivation, soreness, joint pain, hydration, protein, steps, ready-to-train).
- [ ] Android: Retrofit client pointed at your backend URL; submit the form, display the coach's response.
- [ ] Android: `WorkManager` periodic job that fires a local notification at your chosen check-in time, deep-linking into the check-in screen.
- [ ] Test end to end: submit a real check-in from the phone, confirm the response looks right and the row lands in `daily_checkins`.

**Checkpoint:** you can get a scheduled reminder, tap it, fill the check-in, and get a coaching response — entirely without Health Connect, location, or FCM. This is the minimum viable app.

## Phase 2 — Health Connect integration

- [ ] Install the Health Connect app on your phone if not already present (required on some Android versions; built-in on others).
- [ ] Declare Health Connect permissions in `AndroidManifest.xml` (Steps, ExerciseSession) and request them at runtime.
- [ ] Read step/workout data via the Health Connect client SDK.
- [ ] Backend: `POST /sync` — accepts Health Connect records, upserts into `activity_logs` with `source = health_connect`.
- [ ] Android: sync button + a background `WorkManager` job for nightly auto-sync.
- [ ] Test: log some steps via your phone's normal step counter or another fitness app, confirm they appear correctly in Health Connect, then confirm they sync and land in `activity_logs`. Then manually enter a different number for the same date and confirm the manual entry takes precedence (per `AGENT_PROMPT.md`'s conflict rule).

## Phase 3 — Schedule visibility + backend push

- [ ] Backend: `GET /schedule` returning the current `schedule` table contents.
- [ ] Android: a Schedule screen listing upcoming check-ins/training days/reports.
- [ ] Create a Firebase project, add `google-services.json` to the Android app, integrate FCM.
- [ ] Backend: install `firebase-admin`, send a push notification whenever the `schedule` table changes.
- [ ] Test: manually edit a row in `schedule` on the backend, confirm a push notification arrives on the phone even with the app closed.

## Phase 4 — Adaptive scheduler

- [ ] Define concrete adherence thresholds before writing the logic — the design doc deliberately left this open. A reasonable starting point: if workout completion drops below ~70% in a week, or 2+ check-ins are missed, add a mid-week accountability reminder; if 2 consecutive weeks are off-track, flag it in the weekly report for the coach persona's plateau-analysis behavior to pick up. Adjust these once you see real data — don't over-tune before you have any.
- [ ] Backend: `node-cron` weekly job evaluating recent `daily_checkins`/`workout_logs` against the thresholds, updating `schedule`, triggering the Phase 3 push.
- [ ] Hard rule to enforce in code, not just prompt text: this job may only write to `schedule`, never to `workout_plan`.
- [ ] Test with seeded synthetic data: an "on-track" week (no schedule change expected) and an "off-track" week (schedule change expected) — assert the behavior explicitly rather than eyeballing it.

## Phase 5 — Location-based activity detection (optional — decide after Phase 1-4 are working)

- [ ] Integrate the Activity Recognition Transition API for automatic still/walking/running/in-vehicle detection.
- [ ] Optionally add one geofence around your gym's coordinates.
- [ ] Implement the "Allow all the time" background location permission flow explicitly (Android requires a distinct settings-page step beyond the normal permission dialog).
- [ ] Run detection inside a foreground service so Android's battery optimization doesn't kill it silently.
- [ ] Test the permission-denied and permission-revoked paths specifically — the app must fall back to manual/Health Connect entry gracefully, not break.

## Phase 6 — Reporting and charts

- [ ] Backend: `node-cron` jobs for weekly (Sundays) and monthly (1st) report generation, matching the metrics `AGENT_PROMPT.md` already specifies.
- [ ] Pick one: native charts (MPAndroidChart, no network round trip) or backend-generated chart images (QuickChart.io, simpler to build, costs a request each time).
- [ ] Test: seed a full week of realistic data, confirm the generated report's numbers are actually correct — check the math by hand once, don't just trust it looks plausible.

## Phase 7 — Hardening

- [ ] Retry logic on failed `/sync` and `/checkin` calls (phone loses signal, backend restarts, etc.).
- [ ] Confirm `WorkManager` jobs survive a phone reboot (test by actually rebooting the phone).
- [ ] Nightly backup of the backend's SQLite file (a cron job copying it somewhere durable is enough at this scale).
- [ ] Build and sideload the release APK: `./gradlew assembleRelease` (or Build → Generate Signed Bundle/APK in Android Studio), then `adb install app-release.apk`, or transfer the file directly and enable "Install unknown apps" for whichever app you use to open it.

---

## What to do first, right now

Phase 0 and Phase 1 are the only blocking work before you have something real on your phone. Everything from Phase 2 onward is additive. If you want, I can scaffold the actual backend code (`Phase 0`–`1`: Express server, SQLite schema, the `/checkin` route calling Claude) right now — that's a concrete, boundable piece of work I can do directly, whereas the Android app needs Android Studio on your machine and can't be built from here.
