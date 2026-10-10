// server/src/routes/ocrRoutes.js
// OCR endpoints for extracting text from ordinary images/documents.
// Isolated, non-blocking, and handles engine absence/timeouts gracefully.

const express = require('express');
const router = express.Router();
const ocrService = require('../ocr/ocrService');
const logger = require('../utils/logger');

/**
 * POST /v1/ocr/extract
 * Extracts text from an uploaded image.
 * Accepts { imageBase64, lang, psm, timeout, preprocess, preprocessOptions }
 */
router.post('/extract', async (req, res, next) => {
  try {
    const { imageBase64, imageBuffer: rawBufferInput, lang, psm, timeout, preprocess, preprocessOptions } = req.body || {};

    let buffer;
    if (imageBase64) {
      // Strip optional data URI scheme prefix (e.g., data:image/png;base64,)
      const cleanBase64 = imageBase64.replace(/^data:image\/[a-z0-9-+.]+;base64,/, '');
      buffer = Buffer.from(cleanBase64, 'base64');
    } else if (rawBufferInput && Buffer.isBuffer(rawBufferInput)) {
      buffer = rawBufferInput;
    } else {
      return res.status(400).json({
        error: 'INVALID_REQUEST',
        message: 'imageBase64 is required to perform text extraction.'
      });
    }

    if (buffer.length === 0) {
      return res.status(400).json({
        error: 'EMPTY_IMAGE',
        message: 'Decoded image buffer is empty.'
      });
    }

    const result = await ocrService.extractText(buffer, {
      lang,
      psm,
      timeout,
      preprocess,
      preprocessOptions
    });

    return res.status(200).json({
      success: true,
      data: {
        text: result.text,
        durationMs: result.durationMs,
        preprocessed: result.preprocessed,
        lang: result.lang,
        psm: result.psm
      }
    });
  } catch (err) {
    if (err.message && err.message.startsWith('IMAGE_TOO_LARGE')) {
      return res.status(413).json({
        error: 'IMAGE_TOO_LARGE',
        message: err.message
      });
    }

    if (err.message && (err.message.startsWith('UNSUPPORTED_FORMAT') || err.message.startsWith('EMPTY_IMAGE') || err.message.startsWith('INVALID_INPUT'))) {
      return res.status(400).json({
        error: 'INVALID_IMAGE_FORMAT',
        message: err.message
      });
    }

    if (err.code === 'TESSERACT_BINARY_NOT_FOUND') {
      return res.status(503).json({
        error: 'OCR_SERVICE_UNAVAILABLE',
        message: err.message
      });
    }

    if (err.code === 'OCR_TIMEOUT') {
      return res.status(504).json({
        error: 'OCR_TIMEOUT',
        message: err.message
      });
    }

    if (err.code === 'LANGUAGE_NOT_FOUND') {
      return res.status(422).json({
        error: 'LANGUAGE_NOT_FOUND',
        message: err.message
      });
    }

    logger.error('OCR extraction failed', { error: err.message });
    return res.status(500).json({
      error: 'OCR_PROCESSING_FAILED',
      message: err.message || 'Failed to extract text from image.'
    });
  }
});

/**
 * GET /v1/ocr/status
 * Health check for OCR service, reports if local Tesseract engine is ready.
 */
router.get('/status', async (req, res) => {
  const isAvailable = await ocrService.checkBinaryAvailable();
  return res.status(200).json({
    engine: 'tesseract',
    available: isAvailable,
    binaryPath: ocrService.tesseractPath,
    defaultLang: ocrService.defaultLang,
    defaultPsm: ocrService.defaultPsm
  });
});

module.exports = router;
