// server/src/middleware/authMiddleware.js
// Validates Bearer tokens and attaches session without leaking cookies to the client.

const sessionStore = require('../ims/sessionStore');
const logger = require('../utils/logger');

function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Missing or invalid Authorization header. Expected Bearer token.'
    });
  }

  const token = authHeader.substring(7).trim();
  if (!token) {
    return res.status(401).json({
      error: 'UNAUTHORIZED',
      message: 'Empty authorization token provided.'
    });
  }

  const session = sessionStore.getSession(token);
  if (!session) {
    return res.status(401).json({
      error: 'SESSION_EXPIRED',
      message: 'Session has expired or is invalid. Please log in again.'
    });
  }

  // Attach internal session for backend routes (contains backend cookies, never returned in res)
  req.session = session;
  req.sessionToken = token;
  next();
}

module.exports = {
  requireAuth
};
