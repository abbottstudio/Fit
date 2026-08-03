const express = require('express');
const db = require('../db');
const { askCoach } = require('../claude');
const { extractStructuredLog } = require('../structuredOutput');

const router = express.Router();

router.post('/checkin', async (req, res) => {
  const {
    date,
    weight_kg,
    sleep_hours,
    energy,
    stress,
    motivation,
    soreness,
    joint_pain,
    hydration_l,
    protein_g,
    steps,
    ready_to_train,
    message, // optional freeform message instead of/in addition to the structured fields
  } = req.body || {};

  if (!date) {
    return res.status(400).json({ error: 'date is required (YYYY-MM-DD)' });
  }

  const recentCheckins = db
    .prepare(
      `SELECT date, weight_kg, sleep_hours, energy, stress, motivation, steps, ready_to_train
       FROM daily_checkins ORDER BY date DESC LIMIT 7`
    )
    .all();

  const contextBlock = [
    `Today's date: ${date}`,
    `Recent check-in history (most recent first, JSON): ${JSON.stringify(recentCheckins)}`,
  ].join('\n');

  const userMessage =
    message ||
    [
      'Daily check-in:',
      `Weight: ${weight_kg ?? 'not provided'} kg`,
      `Sleep: ${sleep_hours ?? 'not provided'} hours`,
      `Energy: ${energy ?? 'not provided'}/10, Stress: ${stress ?? 'not provided'}/10, Motivation: ${motivation ?? 'not provided'}/10`,
      `Soreness: ${soreness ?? 'not provided'}, Joint pain: ${joint_pain ?? 'not provided'}`,
      `Hydration yesterday: ${hydration_l ?? 'not provided'} L, Protein yesterday: ${protein_g ?? 'not provided'} g`,
      `Steps yesterday: ${steps ?? 'not provided'}`,
      `Ready to train: ${ready_to_train ? 'yes' : 'no'}`,
    ].join('\n');

  try {
    const rawResponse = await askCoach({ userMessage, contextBlock });
    const { humanText, structured } = extractStructuredLog(rawResponse);

    db.prepare(
      `INSERT INTO daily_checkins
        (date, weight_kg, sleep_hours, energy, stress, motivation, soreness, joint_pain, hydration_l, protein_g, steps, ready_to_train, raw_response)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      date,
      weight_kg ?? null,
      sleep_hours ?? null,
      energy ?? null,
      stress ?? null,
      motivation ?? null,
      soreness ?? null,
      joint_pain ?? null,
      hydration_l ?? null,
      protein_g ?? null,
      steps ?? null,
      ready_to_train ? 1 : 0,
      rawResponse
    );

    res.json({ reply: humanText, structured });
  } catch (err) {
    console.error('POST /checkin failed:', err);
    res.status(500).json({ error: 'coach_request_failed', detail: err.message });
  }
});

module.exports = router;
