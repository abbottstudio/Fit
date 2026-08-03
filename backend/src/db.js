const fs = require('fs');
const path = require('path');
const Database = require('better-sqlite3');

const DB_PATH = process.env.DB_PATH
  ? path.resolve(process.cwd(), process.env.DB_PATH)
  : path.join(__dirname, '..', 'data', 'fitcoach.db');

fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });

const db = new Database(DB_PATH);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

const schemaPath = path.join(__dirname, '..', 'db', 'schema.sql');
const schema = fs.readFileSync(schemaPath, 'utf8');
db.exec(schema);

// Ensure the single config row exists (this is a single-user app - see CLAUDE.md).
const configRow = db.prepare('SELECT id FROM config WHERE id = 1').get();
if (!configRow) {
  db.prepare(
    'INSERT INTO config (id, plan_start_date, timezone) VALUES (1, ?, ?)'
  ).run(new Date().toISOString().slice(0, 10), 'Asia/Kolkata');
}

module.exports = db;
