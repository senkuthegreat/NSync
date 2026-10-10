// server/src/ocr/ocrService.js
// Isolated, non-blocking OCR service using local Tesseract engine.
// Handles timeouts, missing binaries, and configuration options gracefully.

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const logger = require('../utils/logger');
const { preprocessImage } = require('./imagePreprocessor');

const DEFAULT_TIMEOUT_MS = 8000;
const DEFAULT_LANG = 'eng';
const DEFAULT_PSM = 3;

class OcrService {
  constructor(options = {}) {
    this.tesseractPath = options.tesseractPath || process.env.TESSERACT_PATH || this._findDefaultBinaryPath();
    this.defaultLang = options.defaultLang || process.env.TESSERACT_LANG || DEFAULT_LANG;
    this.defaultPsm = options.defaultPsm || process.env.TESSERACT_PSM || DEFAULT_PSM;
    this.defaultTimeoutMs = options.defaultTimeoutMs || Number(process.env.OCR_TIMEOUT_MS) || DEFAULT_TIMEOUT_MS;
  }

  /**
   * Helper to inspect standard installation locations if tesseract isn't directly on PATH.
   */
  _findDefaultBinaryPath() {
    if (process.platform === 'win32') {
      const candidates = [
        'tesseract',
        'C:\\Program Files\\Tesseract-OCR\\tesseract.exe',
        'C:\\Program Files (x86)\\Tesseract-OCR\\tesseract.exe',
        path.join(process.env.LOCALAPPDATA || '', 'Programs', 'Tesseract-OCR', 'tesseract.exe')
      ];
      for (const candidate of candidates) {
        if (candidate === 'tesseract') continue;
        if (fs.existsSync(candidate)) {
          return candidate;
        }
      }
    }
    return 'tesseract';
  }

  /**
   * Checks if the Tesseract binary is available and runnable.
   */
  async checkBinaryAvailable() {
    return new Promise(resolve => {
      const proc = spawn(this.tesseractPath, ['--version']);
      proc.on('error', () => resolve(false));
      proc.on('close', code => resolve(code === 0));
    });
  }

  /**
   * Extracts text from an image buffer using local Tesseract.
   * Image can optionally be preprocessed with OpenCV first.
   *
   * @param {Buffer} imageBuffer - Raw image bytes
   * @param {Object} options - { lang, psm, timeout, preprocess, preprocessOptions }
   * @returns {Promise<{ text: string, durationMs: number, preprocessed: boolean }>}
   */
  async extractText(imageBuffer, options = {}) {
    const startTime = Date.now();
    const shouldPreprocess = options.preprocess !== false; // Default true
    let workingBuffer = imageBuffer;

    if (shouldPreprocess) {
      try {
        workingBuffer = await preprocessImage(imageBuffer, options.preprocessOptions || {});
      } catch (preprocessErr) {
        logger.warn('Preprocessing skipped or failed, falling back to original image buffer', {
          error: preprocessErr.message
        });
        workingBuffer = imageBuffer;
      }
    }

    const lang = options.lang || this.defaultLang;
    const psm = String(options.psm || this.defaultPsm);
    const timeoutMs = options.timeout || this.defaultTimeoutMs;

    return new Promise((resolve, reject) => {
      // Arguments: stdin stdout -l <lang> --psm <psm>
      // modern tesseract takes 'stdin' as input and 'stdout' as output
      const args = ['stdin', 'stdout', '-l', lang, '--psm', psm];

      let proc;
      let timer = null;
      let hasTimedOut = false;

      try {
        proc = spawn(this.tesseractPath, args, {
          stdio: ['pipe', 'pipe', 'pipe']
        });
      } catch (err) {
        const error = new Error(`Failed to spawn Tesseract: ${err.message}`);
        error.code = 'TESSERACT_SPAWN_ERROR';
        return reject(error);
      }

      // Timeout watchdog
      timer = setTimeout(() => {
        hasTimedOut = true;
        try {
          proc.kill('SIGKILL');
        } catch (_) {}
        const timeoutError = new Error(`OCR processing timed out after ${timeoutMs}ms`);
        timeoutError.code = 'OCR_TIMEOUT';
        reject(timeoutError);
      }, timeoutMs);

      const stdoutChunks = [];
      let stderrText = '';

      proc.stdout.on('data', chunk => {
        stdoutChunks.push(chunk);
      });

      proc.stderr.on('data', chunk => {
        stderrText += chunk.toString();
      });

      proc.on('error', err => {
        clearTimeout(timer);
        if (err.code === 'ENOENT') {
          const binaryNotFoundErr = new Error(
            `Tesseract OCR binary not found at "${this.tesseractPath}". Please ensure Tesseract is installed and available in PATH, or configure TESSERACT_PATH.`
          );
          binaryNotFoundErr.code = 'TESSERACT_BINARY_NOT_FOUND';
          return reject(binaryNotFoundErr);
        }
        const spawnErr = new Error(`OCR process execution error: ${err.message}`);
        spawnErr.code = 'OCR_PROCESS_ERROR';
        reject(spawnErr);
      });

      proc.on('close', code => {
        clearTimeout(timer);
        if (hasTimedOut) return;

        if (code !== 0) {
          // Check for missing language data
          if (stderrText.includes('Failed loading language') || stderrText.includes('Error opening data file')) {
            const langErr = new Error(`Tesseract language traineddata for '${lang}' is not installed`);
            langErr.code = 'LANGUAGE_NOT_FOUND';
            return reject(langErr);
          }

          const failErr = new Error(`OCR process exited with code ${code}: ${stderrText.trim()}`);
          failErr.code = 'OCR_EXECUTION_FAILED';
          return reject(failErr);
        }

        const rawText = Buffer.concat(stdoutChunks).toString('utf-8');
        const cleanedText = rawText.trim();
        const durationMs = Date.now() - startTime;

        logger.info('OCR text extraction completed', {
          durationMs,
          textLength: cleanedText.length,
          preprocessed: shouldPreprocess
        });

        resolve({
          text: cleanedText,
          durationMs,
          preprocessed: shouldPreprocess,
          lang,
          psm
        });
      });

      // Write image buffer to stdin and close stdin stream
      proc.stdin.write(workingBuffer);
      proc.stdin.end();
    });
  }
}

const defaultOcr = new OcrService();
module.exports = defaultOcr;
module.exports.OcrService = OcrService;
