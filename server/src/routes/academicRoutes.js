// server/src/routes/academicRoutes.js
// Protected academic routes reusing active IMS sessions without repeated authentication.

const express = require('express');
const router = express.Router();
const { requireAuth } = require('../middleware/authMiddleware');
const imsClient = require('../ims/imsClient');
const sessionStore = require('../ims/sessionStore');
const logger = require('../utils/logger');

// All academic routes require valid session token
router.use(requireAuth);

/**
 * GET /v1/academic/attendance
 * Retrieves attendance using active IMS session.
 */
router.get('/attendance', async (req, res, next) => {
  try {
    const cookies = req.session.cookies;
    logger.info(`Fetching attendance for user ${req.session.userId} reusing session`);
    const attendance = await imsClient.fetchAttendance(cookies);

    return res.status(200).json({
      success: true,
      userId: req.session.userId,
      data: attendance
    });
  } catch (err) {
    if (err.code === 'SESSION_EXPIRED') {
      logger.warn(`IMS session expired on upstream server for user ${req.session.userId}`);
      // Invalidate expired session in store
      sessionStore.revokeSession(req.sessionToken);
      return res.status(401).json({
        error: 'SESSION_EXPIRED',
        message: 'Your IMS portal session has expired. Please re-authenticate with a new CAPTCHA.'
      });
    }

    logger.error('Failed to retrieve attendance', { error: err.message });
    return res.status(502).json({
      error: 'IMS_GATEWAY_ERROR',
      message: 'Failed to retrieve attendance from IMS server.'
    });
  }
});

/**
 * GET /v1/academic/timetable
 * Retrieves timetable using active IMS session.
 */
router.get('/timetable', async (req, res, next) => {
  try {
    const cookies = req.session.cookies;
    logger.info(`Fetching timetable for user ${req.session.userId} reusing session`);
    const timetable = await imsClient.fetchTimetable(cookies);

    return res.status(200).json({
      success: true,
      userId: req.session.userId,
      data: timetable
    });
  } catch (err) {
    if (err.code === 'SESSION_EXPIRED') {
      logger.warn(`IMS session expired on upstream server for user ${req.session.userId}`);
      sessionStore.revokeSession(req.sessionToken);
      return res.status(401).json({
        error: 'SESSION_EXPIRED',
        message: 'Your IMS portal session has expired. Please re-authenticate with a new CAPTCHA.'
      });
    }

    logger.error('Failed to retrieve timetable', { error: err.message });
    return res.status(502).json({
      error: 'IMS_GATEWAY_ERROR',
      message: 'Failed to retrieve timetable from IMS server.'
    });
  }
});

module.exports = router;
