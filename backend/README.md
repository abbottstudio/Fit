# FitCoach Pro — Backend

Implements Phase 0 and Phase 1 from `../IMPLEMENTATION_STEPS.md`: an Express + SQLite server that runs the coaching persona from `../AGENT_PROMPT.md` against the Claude API and logs the result.

## Setup

1. `npm install`
2. Copy `.env.example` to `.env` and fill in:
   - `ANTHROPIC_API_KEY` — from console.anthropic.com
   - `API_SHARED_SECRET` — any long random string. The Android app must send this exact value as a Bearer token on every request; without it, this server is an open, unauthenticated gateway to a paid API.
3. The real plan is already in place — `data/workout_plan.md` (full text, loaded as system context on every request) and `data/plan_data.json` (phase milestones + baseline stats). Run:
   ```
   npm run seed-plan
   ```
   to load the phase milestones and baseline stats (starting weight/height/waist) into the DB. Safe to re-run; it won't duplicate rows or overwrite config values you've since changed by hand.
4. `npm start` — listens on port 3000 (or `PORT` from `.env`).

## Test it without the Android app

```bash
curl http://localhost:3000/
# -> {"status":"ok","service":"fitcoach-pro-backend","time":"..."}

curl -X POST http://localhost:3000/checkin \
  -H "Authorization: Bearer YOUR_API_SHARED_SECRET" \
  -H "Content-Type: application/json" \
  -d '{
    "date": "2026-08-02",
    "weight_kg": 114.5,
    "sleep_hours": 6.5,
    "energy": 7,
    "stress": 4,
    "motivation": 8,
    "soreness": "moderate",
    "joint_pain": "none",
    "hydration_l": 2.8,
    "protein_g": 110,
    "steps": 8200,
    "ready_to_train": true
  }'
```

Expect back `{ "reply": "...", "structured": {...} }`. Check `data/fitcoach.db` (open with any SQLite browser, or `sqlite3 data/fitcoach.db "select * from daily_checkins;"`) to confirm the row landed.

## What's here

- `db/schema.sql` — matches the data model in `../CLAUDE.md`. `plan_phases` holds the 3 weight milestones (105/95/90 kg); the plan text itself isn't in the DB (see below).
- `data/workout_plan.md` — the real 18-month plan, transcribed from the uploaded PDF. This is what the coach actually reasons from.
- `data/plan_data.json` — structured facts pulled from the same plan (phase milestones, nutrition targets, weekly schedule, steps targets) for the DB/scheduler to query without re-parsing the document.
- `src/db.js` — opens the SQLite file, runs the schema, ensures the single `config` row exists.
- `src/claude.js` — extracts the system prompt out of `../AGENT_PROMPT.md`, loads `data/workout_plan.md`, and sends both as separate cached system blocks on every Messages API call.
- `src/structuredOutput.js` — strips and parses the trailing ` ```json ` block the persona is instructed to emit, per `AGENT_PROMPT.md`'s structured-output contract.
- `src/middleware/auth.js` — bearer-token check, fails closed if the secret isn't configured.
- `src/routes/checkin.js` — `POST /checkin`.
- `scripts/seedWorkoutPlan.js` — loads `plan_data.json`'s phase milestones and baseline stats into the DB.

## Not yet implemented

Everything from Phase 2 onward in `../IMPLEMENTATION_STEPS.md`: `/sync` (Health Connect), `/schedule`, FCM push, the adaptive scheduler, weekly/monthly reporting. This is deliberately scoped to just prove the check-in → Claude → logged-response loop before the Android app exists to call it.
