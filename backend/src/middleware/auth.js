/**
 * Every route except GET / requires this. Single shared secret is enough for
 * a single-user app - this is not a multi-tenant auth system, just a gate to
 * stop randoms who find the server URL from calling the (paid) Claude API.
 */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;

  if (!process.env.API_SHARED_SECRET) {
    // Fail closed, not open - a misconfigured server should refuse
    // everything rather than silently accept unauthenticated requests.
    return res.status(500).json({ error: 'server_misconfigured', detail: 'API_SHARED_SECRET is not set' });
  }

  if (!token || token !== process.env.API_SHARED_SECRET) {
    return res.status(401).json({ error: 'unauthorized' });
  }

  next();
}

module.exports = requireAuth;
