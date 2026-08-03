/**
 * One-time seed: loads data/plan_data.json (structured facts extracted from
 * the real plan in data/workout_plan.md) into `plan_phases` and `config`.
 *
 * The full plan text itself doesn't need seeding - src/claude.js reads
 * data/workout_plan.md directly on every request. This script just populates
 * the small amount of structured data the app/scheduler need without
 * re-parsing the document (current weight vs. phase milestones, baseline
 * stats).
 *
 * Safe to re-run: it skips seeding plan_phases if rows already exist, and
 * only fills in config columns that are still empty.
 */
require('dotenv').config();
const fs = require('fs');
const path = require('path');
const db = require('../src/db');

const planDataPath = path.join(__dirname, '..', 'data', 'plan_data.json');
const planData = JSON.parse(fs.readFileSync(planDataPath, 'utf8'));

const existingPhases = db.prepare('SELECT COUNT(*) AS n FROM plan_phases').get();
if (existingPhases.n > 0) {
  console.log(`plan_phases already has ${existingPhases.n} row(s) - skipping phase seed. Delete rows manually first if you want to reseed.`);
} else {
  const insertPhase = db.prepare(
    'INSERT INTO plan_phases (phase_number, target_weight_kg, label) VALUES (?, ?, ?)'
  );
  const insertPhases = db.transaction((phases) => {
    for (const p of phases) {
      insertPhase.run(p.phase_number, p.target_weight_kg, p.label);
    }
  });
  insertPhases(planData.phases);
  console.log(`Seeded ${planData.phases.length} plan_phases rows.`);
}

db.prepare(
  `UPDATE config
   SET start_weight_kg = COALESCE(start_weight_kg, ?),
       height_cm = COALESCE(height_cm, ?),
       start_waist_cm = COALESCE(start_waist_cm, ?)
   WHERE id = 1`
).run(planData.profile.start_weight_kg, planData.profile.height_cm, planData.profile.start_waist_cm);

console.log('Updated config with baseline profile stats (only where previously empty).');
console.log('Done. The full plan text is read directly from data/workout_plan.md at request time - nothing more to seed for that.');
