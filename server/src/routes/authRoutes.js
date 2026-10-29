// server/src/routes/authRoutes.js
// Authentication endpoints for IMS CAPTCHA challenge delivery and login session lifecycle.

const express = require('express');
const router = express.Router();
const imsClient = require('../ims/imsClient');
const sessionStore = require('../ims/sessionStore');
const logger = require('../utils/logger');

/**
 * POST /v1/auth/challenges
 * Fetch an official IMS CAPTCHA challenge for display in the app UI.
 */
router.post('/challenges', async (req, res, next) => {
  try {
    const challenge = await imsClient.fetchCaptchaChallenge();
    return res.status(200).json({
      success: true,
      challengeId: challenge.challengeId,
      captchaImage: challenge.captchaImage,
      expiresAt: challenge.expiresAt
    });
  } catch (err) {
    logger.error('Failed to generate CAPTCHA challenge', { error: err.message });
    return res.status(502).json({
      error: 'IMS_UNAVAILABLE',
      message: 'Failed to contact NSUT IMS portal to generate CAPTCHA. Please try again.'
    });
  }
});

/**
 * POST /v1/auth/challenges/refresh
 * Refresh an existing or unreadable CAPTCHA challenge.
 */
router.post('/challenges/refresh', async (req, res, next) => {
  try {
    const { challengeId } = req.body || {};
    const challenge = await imsClient.refreshCaptchaChallenge(challengeId);
    return res.status(200).json({
      success: true,
      challengeId: challenge.challengeId,
      captchaImage: challenge.captchaImage,
      expiresAt: challenge.expiresAt
    });
  } catch (err) {
    logger.error('Failed to refresh CAPTCHA challenge', { error: err.message });
    return res.status(502).json({
      error: 'IMS_UNAVAILABLE',
      message: 'Failed to refresh CAPTCHA challenge.'
    });
  }
});

/**
 * POST /v1/auth/challenges/auto-solve
 * Automatically solves a CAPTCHA challenge using OpenCV preprocessing and Tesseract OCR.
 */
router.post('/challenges/auto-solve', async (req, res, next) => {
  try {
    const { challengeId } = req.body || {};
    if (!challengeId) {
      return res.status(400).json({
        error: 'INVALID_REQUEST',
        message: 'challengeId is required.'
      });
    }

    const solvedCaptcha = await imsClient.solveChallenge(challengeId);
    return res.status(200).json({
      success: true,
      challengeId,
      solvedCaptcha
    });
  } catch (err) {
    if (err.code === 'CHALLENGE_EXPIRED') {
      return res.status(410).json({
        error: 'CHALLENGE_EXPIRED',
        message: 'CAPTCHA challenge has expired. Please refresh the CAPTCHA.'
      });
    }
    logger.error('Failed to auto-solve CAPTCHA challenge', { error: err.message });
    return res.status(500).json({
      error: 'OCR_SOLVE_FAILED',
      message: err.message || 'Failed to auto-solve CAPTCHA challenge.'
    });
  }
});

/**
 * POST /v1/auth/auto-login
 * Automatically authenticates into IMS using OpenCV + Tesseract OCR to solve CAPTCHA.
 */
router.post('/auto-login', async (req, res, next) => {
  try {
    const { username, password } = req.body || {};
    if (!username || !password) {
      return res.status(400).json({
        error: 'INVALID_CREDENTIALS',
        message: 'Username/roll number and password are required.'
      });
    }

    const authResult = await imsClient.autoAuthenticate({
      username: String(username).trim(),
      password
    });

    const session = sessionStore.createSession({
      userId: authResult.userId,
      cookies: authResult.cookies,
      metadata: authResult.metadata
    });

    return res.status(200).json({
      success: true,
      token: session.token,
      user: {
        rollNumber: session.userId,
        ...session.metadata
      },
      expiresAt: session.expiresAt
    });
  } catch (err) {
    if (err.code === 'INVALID_CREDENTIALS') {
      return res.status(401).json({
        error: 'INVALID_CREDENTIALS',
        message: err.message || 'Invalid Roll Number or Password.'
      });
    }
    if (err.code === 'INVALID_CAPTCHA') {
      return res.status(400).json({
        error: 'INVALID_CAPTCHA',
        message: 'Security verification code could not be verified automatically. Please try again.'
      });
    }
    logger.error('Unexpected auto-login failure', { error: err.message });
    return res.status(500).json({
      error: 'AUTO_LOGIN_FAILED',
      message: err.message || 'Failed to auto-login.'
    });
  }
});

/**
 * POST /v1/auth/login
 * Log in to IMS using credentials and human-answered CAPTCHA.
 * Returns an opaque bearer token and stores cookies securely in memory.
 */
router.post('/login', async (req, res, next) => {
  try {
    const { challengeId, username, password, captchaAnswer } = req.body || {};

    if (!challengeId) {
      return res.status(400).json({
        error: 'INVALID_REQUEST',
        message: 'challengeId is required.'
      });
    }

    if (!username || !password) {
      return res.status(400).json({
        error: 'INVALID_CREDENTIALS',
        message: 'Username/roll number and password are required.'
      });
    }

    if (!captchaAnswer || String(captchaAnswer).trim() === '') {
      return res.status(400).json({
        error: 'INVALID_CAPTCHA',
        message: 'CAPTCHA security code answer is required.'
      });
    }

    // Authenticate with IMS
    const authResult = await imsClient.authenticate({
      challengeId,
      username: String(username).trim(),
      password,
      captchaAnswer: String(captchaAnswer).trim()
    });

    // Create session in session store (keeps cookies private in memory)
    const session = sessionStore.createSession({
      userId: authResult.userId,
      cookies: authResult.cookies,
      metadata: authResult.metadata
    });

    return res.status(200).json({
      success: true,
      token: session.token,
      user: {
        rollNumber: session.userId,
        ...session.metadata
      },
      expiresAt: session.expiresAt
    });
  } catch (err) {
    if (err.code === 'INVALID_CAPTCHA') {
      return res.status(400).json({
        error: 'INVALID_CAPTCHA',
        message: err.message || 'Incorrect CAPTCHA answer entered. Please check and try again.'
      });
    }

    if (err.code === 'CHALLENGE_EXPIRED') {
      return res.status(410).json({
        error: 'CHALLENGE_EXPIRED',
        message: 'CAPTCHA challenge has expired. Please refresh the CAPTCHA.'
      });
    }

    if (err.code === 'INVALID_CREDENTIALS') {
      return res.status(401).json({
        error: 'INVALID_CREDENTIALS',
        message: err.message || 'Invalid Roll Number or Password.'
      });
    }

    if (err.code === 'IMS_NETWORK_ERROR') {
      return res.status(502).json({
        error: 'IMS_GATEWAY_ERROR',
        message: 'Could not communicate with IMS server. Please verify your connection.'
      });
    }

    logger.error('Unexpected login failure', { error: err.message });
    return res.status(500).json({
      error: 'INTERNAL_ERROR',
      message: 'An unexpected error occurred during authentication.'
    });
  }
});

/**
 * POST /v1/auth/renew
 * Renews the validity of an existing active session token.
 */
router.post('/renew', (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ')
    ? authHeader.substring(7).trim()
    : (req.body && req.body.token);

  if (!token) {
    return res.status(400).json({
      error: 'INVALID_REQUEST',
      message: 'Token is required to renew session.'
    });
  }

  const renewed = sessionStore.renewSession(token);
  if (!renewed) {
    return res.status(401).json({
      error: 'SESSION_EXPIRED',
      message: 'Session has expired or does not exist.'
    });
  }

  return res.status(200).json({
    success: true,
    token: renewed.token,
    expiresAt: renewed.expiresAt
  });
});

/**
 * POST /v1/auth/logout
 * Revokes the session token.
 */
router.post('/logout', (req, res) => {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.startsWith('Bearer ')
    ? authHeader.substring(7).trim()
    : (req.body && req.body.token);

  if (token) {
    sessionStore.revokeSession(token);
  }

  return res.status(200).json({
    success: true,
    message: 'Session revoked successfully.'
  });
});

module.exports = router;
