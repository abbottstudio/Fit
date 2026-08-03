-- FitCoach Pro backend schema. Single-user (self-use) design per CLAUDE.md.

CREATE TABLE IF NOT EXISTS config (
  id INTEGER PRIMARY KEY CHECK (id = 1),
  plan_start_date TEXT,
  timezone TEXT DEFAULT 'Asia/Kolkata',
  gym_lat REAL,
  gym_lng REAL,
  start_weight_kg REAL,
  height_cm REAL,
  start_waist_cm REAL
);

-- The plan itself is NOT stored day-by-day here - it's not that kind of plan.
-- It's a repeating weekly template (see plan_data.json's weekly_schedule) with
-- phase-based weight milestones. The full authoritative text lives in
-- data/workout_plan.md and is loaded directly by src/claude.js as system
-- context. This table just tracks the phase milestones so the app/scheduler
-- can query progress without re-parsing the document.
CREATE TABLE IF NOT EXISTS plan_phases (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  phase_number INTEGER NOT NULL,
  target_weight_kg REAL NOT NULL,
  label TEXT,
  achieved INTEGER DEFAULT 0,
  achieved_date TEXT
);

CREATE TABLE IF NOT EXISTS daily_checkins (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  weight_kg REAL,
  sleep_hours REAL,
  energy INTEGER,
  stress INTEGER,
  motivation INTEGER,
  soreness TEXT,
  joint_pain TEXT,
  hydration_l REAL,
  protein_g REAL,
  steps INTEGER,
  ready_to_train INTEGER,
  raw_response TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS workout_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  exercise TEXT,
  weight_kg REAL,
  sets INTEGER,
  reps INTEGER,
  rpe REAL,
  technique_notes TEXT,
  pr INTEGER DEFAULT 0,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS nutrition_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  calories REAL,
  protein_g REAL,
  water_l REAL,
  adherence_notes TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS activity_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  date TEXT NOT NULL,
  source TEXT CHECK(source IN ('health_connect','manual','location_geofence')),
  type TEXT,
  duration_min REAL,
  steps INTEGER,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS schedule (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  task_type TEXT NOT NULL,
  scheduled_time TEXT,
  frequency TEXT,
  active INTEGER DEFAULT 1,
  updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS notifications_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  type TEXT,
  sent_at TEXT DEFAULT (datetime('now')),
  status TEXT
);

CREATE TABLE IF NOT EXISTS weekly_reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  week_start TEXT,
  metrics_json TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS monthly_reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  month TEXT,
  metrics_json TEXT,
  created_at TEXT DEFAULT (datetime('now'))
);
