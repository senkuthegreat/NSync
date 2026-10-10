// server/src/ocr/imagePreprocessor.js
// Validates image formats and streams through the OpenCV preprocessor without temp files.

const { spawn } = require('child_process');
const path = require('path');
const logger = require('../utils/logger');

const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB limit
const PYTHON_SCRIPT_PATH = path.join(__dirname, 'imagePreprocessor.py');

/**
 * Validates image buffer magic numbers.
 * Supported: PNG, JPEG, WebP, TIFF.
 */
function validateImageBuffer(buffer) {
  if (!Buffer.isBuffer(buffer)) {
    throw new Error('INVALID_INPUT: Expected Buffer');
  }

  if (buffer.length === 0) {
    throw new Error('EMPTY_IMAGE: Image buffer is empty');
  }

  if (buffer.length > MAX_IMAGE_SIZE_BYTES) {
    throw new Error(`IMAGE_TOO_LARGE: Image size (${(buffer.length / (1024 * 1024)).toFixed(2)} MB) exceeds maximum allowed limit of 5 MB`);
  }

  // PNG
  if (buffer.length >= 8 &&
      buffer[0] === 0x89 && buffer[1] === 0x50 && buffer[2] === 0x4E && buffer[3] === 0x47 &&
      buffer[4] === 0x0D && buffer[5] === 0x0A && buffer[6] === 0x1A && buffer[7] === 0x0A) {
    return { format: 'png', mimeType: 'image/png' };
  }

  // JPEG
  if (buffer.length >= 3 &&
      buffer[0] === 0xFF && buffer[1] === 0xD8 && buffer[2] === 0xFF) {
    return { format: 'jpeg', mimeType: 'image/jpeg' };
  }

  // WebP: 'RIFF'....'WEBP'
  if (buffer.length >= 12 &&
      buffer.toString('ascii', 0, 4) === 'RIFF' &&
      buffer.toString('ascii', 8, 12) === 'WEBP') {
    return { format: 'webp', mimeType: 'image/webp' };
  }

  // TIFF (little-endian or big-endian)
  if (buffer.length >= 4 &&
      ((buffer[0] === 0x49 && buffer[1] === 0x49 && buffer[2] === 0x2A && buffer[3] === 0x00) ||
       (buffer[0] === 0x4D && buffer[1] === 0x4D && buffer[0] === 0x00 && buffer[3] === 0x2A))) {
    return { format: 'tiff', mimeType: 'image/tiff' };
  }

  throw new Error('UNSUPPORTED_FORMAT: Unrecognized or unsupported image format. Supported formats: PNG, JPEG, WebP, TIFF');
}

/**
 * Runs image preprocessing via streaming OpenCV child process.
 * Does not write any temporary files to disk.
 * Non-blocking child_process execution.
 */
function preprocessImage(inputBuffer, options = {}) {
  return new Promise((resolve, reject) => {
    try {
      validateImageBuffer(inputBuffer);
    } catch (err) {
      return reject(err);
    }

    const pythonPath = process.env.PYTHON_PATH || 'python';
    const args = [PYTHON_SCRIPT_PATH];

    if (options.grayscale === false) {
      args.push('--no-grayscale');
    } else {
      args.push('--grayscale');
    }

    if (options.resizeMaxDim) {
      args.push('--resize-max-dim', String(options.resizeMaxDim));
    }

    if (options.adaptiveThreshold) {
      args.push('--adaptive-threshold');
    }

    if (options.denoise) {
      args.push('--denoise');
    }

    if (options.outputFormat) {
      args.push('--output-format', options.outputFormat);
    }

    const proc = spawn(pythonPath, args, {
      stdio: ['pipe', 'pipe', 'pipe']
    });

    const stdoutChunks = [];
    let stderrText = '';

    proc.stdout.on('data', chunk => {
      stdoutChunks.push(chunk);
    });

    proc.stderr.on('data', chunk => {
      stderrText += chunk.toString();
    });

    proc.on('error', err => {
      logger.error('Failed to start python preprocessor process', { error: err.message });
      reject(new Error(`PREPROCESSOR_SPAWN_FAILED: ${err.message}`));
    });

    proc.on('close', code => {
      if (code !== 0) {
        logger.warn('Image preprocessing failed', { code, stderr: stderrText });
        return reject(new Error(`PREPROCESSING_FAILED: ${stderrText.trim() || 'Exit code ' + code}`));
      }

      const resultBuffer = Buffer.concat(stdoutChunks);
      resolve(resultBuffer);
    });

    // Stream input buffer directly to stdin, then close stdin
    proc.stdin.write(inputBuffer);
    proc.stdin.end();
  });
}

module.exports = {
  validateImageBuffer,
  preprocessImage,
  MAX_IMAGE_SIZE_BYTES
};
