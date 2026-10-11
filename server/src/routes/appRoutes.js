// server/src/routes/appRoutes.js
// App update metadata and distribution endpoints.

const express = require('express');
const router = express.Router();

const LATEST_RELEASE_URL = 'https://github.com/senkuthegreat/NSync/releases/latest/download/NSync-latest.apk';

/**
 * GET /v1/app/version
 * Returns latest app version information and release notes.
 */
router.get('/version', (req, res) => {
  res.status(200).json({
    latestVersionName: process.env.APP_LATEST_VERSION || '1.0',
    latestVersionCode: Number(process.env.APP_LATEST_VERSION_CODE) || 1,
    downloadUrl: LATEST_RELEASE_URL,
    releaseTitle: 'NSync Update',
    releaseNotes: 'Performance improvements, seamless IMS session reuse, and lightweight OCR text extraction.',
    minSupportedVersion: '1.0'
  });
});

/**
 * GET /v1/app/download
 * Redirects to the latest APK asset on GitHub.
 */
router.get('/download', (req, res) => {
  res.redirect(302, LATEST_RELEASE_URL);
});

module.exports = router;
