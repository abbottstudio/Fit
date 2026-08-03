# CLAUDE.md — FitCoach Pro

This file governs how Claude Code should understand and build this project. It is the engineering source of truth. The coaching LLM's behavior lives in `AGENT_PROMPT.md` — do not duplicate persona content here, and do not let this file drift from that one.

**Scope: self-use, single user, Android.**

## Architecture decision: native Android app, not a messaging bot

You raised two options — WhatsApp-only, or a native `.apk` that pulls Google Fit directly, uses location to auto-mark activity, sends its own alerts, and syncs to a backend that can adapt the schedule. **I recommend the native app, and it resolves the WhatsApp/Telegram question on its own:**

- Native Android notifications (local `AlarmManager`/`WorkManager` scheduling, or FCM push from the backend) deliver reminders directly to your phone with the same reliability as WhatsApp — no business verification, no template approval, no third-party dependency at all.
- Location-based activity marking and on-device Google Fit/Health Connect access are **only possible from an installed app** — a WhatsApp bot architecture cannot do either, so this requirement settles the decision by itself.
- Because the app itself handles alerts, **WhatsApp becomes optional, not required.** Add it later only if you specifically want coaching conversation inside WhatsApp's chat UI in addition to the app. Given you'll be opening the app anyway for schedule/chart review, I'd skip WhatsApp integration entirely unless you find yourself wanting it after using the app for a few weeks.

This is a real scope change from the earlier bot-based plan: a native Android app is a materially bigger build (Android dev, permission flows, background location handling) than a chat-bot backend. Flagging that up front — the phased plan below is ordered so you get a working app fast (manual data entry + local notifications) before taking on the harder pieces (Health Connect sync, location-based auto-detection, adaptive backend scheduling).

## What this project is

1. An Android app (sideloaded APK — no Play Store distribution needed for self-use) that:
   - Displays the schedule (upcoming check-ins, training days, reports) — visible, not just pushed.
   - Sends local notifications for reminders, independent of network connectivity.
   - Pulls step/activity data from **Health Connect** (see note below), with manual entry as override/fallback.
   - Optionally uses location to auto-detect gym visits or outdoor cardio sessions.
   - Hosts the coaching conversation (calls the backend, which calls Claude API with the persona in `AGENT_PROMPT.md`).
   - Displays progress charts.
2. A backend ("mainframe," in your words) that:
   - Proxies Claude API calls (so the API key never ships inside the APK — a key embedded in an installed app can be extracted, even for a personal-use app it's bad practice to ship it client-side).
   - Stores the canonical log history and generates weekly/monthly reports.
   - Runs the **adaptive scheduler**: if check-in data shows you're off track (missed sessions, stalled weight, poor adherence), it adjusts reminder cadence, timing, and coaching intensity. It does **not** rewrite the workout plan itself — that stays canonical per `AGENT_PROMPT.md`'s existing rule ("adjust coaching intensity, never the plan, unless the user explicitly asks"). Same rule, now enforced at the scheduling layer too.

The attached workout plan PDF remains canonical for all fitness/nutrition content in both layers.

## Important technical note: Google Fit API vs. Health Connect

Google has been deprecating the older Google Fit REST/Android APIs in favor of **Health Connect** (`androidx.health.connect`), an on-device health data store that's now the standard way Android apps read step/activity/workout data. Since you're building a native app anyway, **build directly on Health Connect, not the legacy Google Fit REST API** — it's on-device (no OAuth refresh-token management, no server-side polling needed), and it's the path Google is actively steering developers toward. Verify the current migration status before starting, since deprecation timelines shift, but architecturally this is the right call regardless of exact dates.

## Location-based activity marking — implementation note

"Use location" shouldn't mean continuous raw GPS polling — that drains battery fast and is exactly the kind of background-location use Android actively discourages. Two lighter-weight approaches, either or both:

- **Activity Recognition Transition API**: detects transitions between still/walking/running/in-vehicle automatically, battery-efficient, no explicit location coordinates needed.
- **Geofencing** around one or two known locations (e.g. your gym): fires an event on entry/exit, used just to auto-mark "at the gym" for a training-day check-in, not to track continuous movement.

Background location on Android requires the special "Allow all the time" permission grant (a separate step from normal permission dialogs) and, in practice, a foreground service to keep detection reliable. Since this is sideloaded for personal use, there's no Play Store policy review to clear — but the on-device permission flow still applies and is worth treating as its own build phase rather than bundling into v1.

## Recommended architecture

| Layer | Recommendation | Why |
|---|---|---|
| App | Kotlin + Jetpack Compose | Standard modern Android stack; best support for Health Connect, Activity Recognition, and WorkManager. |
| App-side scheduling/alerts | `WorkManager` + local notifications, backed up by FCM push from the backend | Local scheduling works even if the backend is briefly unreachable; FCM covers backend-triggered alerts (e.g. adaptive schedule changes). |
| Health data | Health Connect (on-device) | See note above — supersedes server-side Google Fit REST polling. |
| Backend | Node.js + TypeScript | Thin proxy for Claude API + sync endpoint + scheduler brain; no need for anything heavier. |
| Database (backend) | SQLite | Single user, low write volume. |
| LLM | Claude API (Messages API), system prompt = `AGENT_PROMPT.md` | Called from backend only, never from the device. |
| Charting | Generate chart images backend-side (QuickChart.io) or render natively in-app (MPAndroidChart) | Either works; native rendering avoids a network round trip for something you'll check often. |

## Data model (backend, single-user schema)

- `config` — plan_start_date, timezone, gym_location (lat/lng, optional), notification_prefs, start_weight_kg, height_cm, start_waist_cm
- The plan itself is **not** stored day-by-day in the DB — the real plan (now attached) turned out to be a repeating weekly template with phase-based weight milestones, not a unique 18-month calendar. The full text lives in `backend/data/workout_plan.md` and is loaded directly by the coach agent service as system context (see below); `plan_phases` (phase_number, target_weight_kg, label, achieved) tracks the milestones so the app can query progress without re-parsing the document.
- `daily_checkins` — date, weight, sleep hours, energy/stress/motivation (1-10), soreness, joint pain, hydration, protein, steps, ready-to-train flag
- `workout_logs` — date, exercise, weight, sets, reps, RPE, technique notes, PR flag
- `nutrition_logs` — date, calories, protein, water, adherence notes
- `activity_logs` — date, source (`health_connect` / `manual` / `location_geofence`), type, duration, steps
- `schedule` — current active reminder schedule (times, frequency) — this is what the adaptive scheduler modifies, never `workout_plan.md` or `plan_phases`
- `notifications_log` — type, sent_at, delivery status
- `weekly_reports` / `monthly_reports` — generated summary + metrics snapshot

## Core services

1. **App: Schedule/Reminder module** — reads `schedule` from backend on sync, sets local `WorkManager` jobs, shows upcoming tasks in-app.
2. **App: Health Connect sync module** — reads steps/activity, uploads to backend on a schedule (e.g. nightly) and on manual refresh.
3. **App: Activity detection module** (Phase 4, not v1) — Activity Recognition + optional geofence, writes to `activity_logs` locally, syncs like the above.
4. **App: Coach chat UI** — sends messages to backend, which forwards to Claude API with `AGENT_PROMPT.md` + injected context, renders the response and strips the structured JSON logging block (see `AGENT_PROMPT.md`).
5. **Backend: Sync endpoint** — receives logs from the app, writes to DB.
6. **Backend: Coach agent service** — Claude API calls, structured-output parsing, DB writes.
7. **Backend: Adaptive scheduler** — evaluates recent adherence/progress against plan milestones on a schedule (e.g. weekly); if off-track, updates the `schedule` table and pushes an FCM notification to the app telling it to re-sync. Bounded by the same "never touch the plan itself" rule as the coaching persona.
8. **Backend: Reporting/chart service** — weekly/monthly aggregation, matches the persona's report formats.

## Dev conventions and safety rules (carried over from standing project preferences)

- Never modify or delete logged data without an explicit confirmation step (soft-delete, not hard-delete).
- Claude API key lives on the backend only — never in the APK, never logged, never committed. `.env.example` with variable names, no values.
- Maintain `CHANGELOG.md` for every file created or materially changed, with timestamp and rationale — extend to code once the build starts.
- Schema migrations affecting existing logged data need explicit sign-off before running.
- All health/nutrition claims trace back to the attached plan or verified science — no invented statistics.
- The adaptive scheduler may change reminder cadence/timing/intensity; it must never alter `workout_plan.md` content without an explicit user-approved change.

## Testing and verification

- Unit test the Health Connect data mapping and the manual-entry override logic.
- Test the adaptive scheduler's decision logic against fixed synthetic check-in histories (on-track, mildly off-track, badly off-track) — verify it never touches `workout_plan.md` or `plan_phases`.
- Test geofence/activity-recognition handling for permission-denied and permission-revoked states (both are common on Android — the app must degrade to manual entry gracefully, not crash or silently stop logging).
- Manually verify local notifications fire correctly after device reboot and under battery-optimization restrictions (a common failure mode for background alarms on Android).

## Open decisions

1. Confirm current Health Connect vs. Google Fit API status before Phase 2 — architecture assumes Health Connect is the live, non-deprecated path.
2. Is location-based activity detection actually worth the added permission complexity, or is manual/Health-Connect-only tracking sufficient? It's scoped as an optional later phase specifically so this can be decided after the core app is working, not before.
3. Hosting for the backend (a small VPS or a free-tier cloud service is enough for single-user traffic) — pick based on what you're already comfortable operating.

## Phased build plan

**Phase 0 — Setup**
~~Transcribe the attached workout plan into structured data~~ — **done**: `backend/data/workout_plan.md` (full text) and `backend/data/plan_data.json` (structured facts). ~~Scaffold the backend~~ — **done**, see `backend/README.md`. Scaffold the Android app project (Kotlin/Compose) — **not started**.

**Phase 1 — Core loop, manual entry, local notifications**
App shows today's check-in as a form (manual entry only, no Health Connect yet), submits to backend, backend calls Claude API and returns the coaching response. Local `WorkManager` notification reminds you to open the app at check-in time. Get this fully working before adding any device-data integration — it's the fastest path to something usable.

**Phase 2 — Health Connect integration**
Read steps/workouts from Health Connect, sync to backend nightly, manual entry becomes an override rather than the only path.

**Phase 3 — Schedule visibility + backend-driven sync**
In-app schedule view backed by the `schedule` table; FCM push wired up so backend-triggered changes (e.g. adaptive rescheduling) reach the app even when it's closed.

**Phase 4 — Adaptive scheduler**
Backend job evaluates adherence weekly, adjusts `schedule` when off-track, matching the plateau-analysis logic already defined in `AGENT_PROMPT.md`.

**Phase 5 — Location-based activity detection (optional)**
Activity Recognition Transition API + gym geofence, background-location permission flow, foreground service for reliability.

**Phase 6 — Reporting and charts**
Weekly/monthly aggregation, in-app or backend-rendered charts.

**Phase 7 — Hardening**
Retry logic on failed syncs, reboot-survival for local notifications, backend DB backup strategy.
