require('dotenv').config();
const express = require('express');
const cors = require('cors');

const requireAuth = require('./middleware/auth');
const checkinRouter = require('./routes/checkin');

if (!process.env.ANTHROPIC_API_KEY) {
  console.error(
    'WARNING: ANTHROPIC_API_KEY is not set (check .env). /checkin will fail until this is fixed.'
  );
}
if (!process.env.API_SHARED_SECRET) {
  console.error(
    'WARNING: API_SHARED_SECRET is not set (check .env). Every protected route will return 401 until this is fixed.'
  );
}

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));

// Unauthenticated - just proves the server is up.
app.get('/', (req, res) => {
  res.json({ status: 'ok', service: 'fitcoach-pro-backend', time: new Date().toISOString() });
});

app.use(requireAuth);
app.use('/', checkinRouter);

// Fallback error handler so unexpected exceptions return JSON, not an HTML stack trace.
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'internal_error' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`FitCoach Pro backend listening on port ${PORT}`);
});
