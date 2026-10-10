// server/test/imagePreprocessor.test.js
const { describe, it } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const { validateImageBuffer, preprocessImage, MAX_IMAGE_SIZE_BYTES } = require('../src/ocr/imagePreprocessor');

const FIXTURE_PATH = path.join(__dirname, 'fixtures', 'sample_text.png');

describe('ImagePreprocessor Unit Tests', () => {
  it('validates correct image formats via magic numbers', () => {
    const pngBuffer = fs.readFileSync(FIXTURE_PATH);
    const validPng = validateImageBuffer(pngBuffer);
    assert.strictEqual(validPng.format, 'png');

    // Synthetic JPEG header
    const jpegBuffer = Buffer.from([0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10, 0x4A, 0x46]);
    const validJpeg = validateImageBuffer(jpegBuffer);
    assert.strictEqual(validJpeg.format, 'jpeg');

    // Synthetic WebP header: 'RIFF' + length + 'WEBP'
    const webpBuffer = Buffer.concat([
      Buffer.from('RIFF', 'ascii'),
      Buffer.from([0x20, 0x00, 0x00, 0x00]),
      Buffer.from('WEBP', 'ascii')
    ]);
    const validWebp = validateImageBuffer(webpBuffer);
    assert.strictEqual(validWebp.format, 'webp');
  });

  it('rejects empty, oversized, or unsupported buffers', () => {
    // Non-buffer
    assert.throws(() => validateImageBuffer('not a buffer'), /INVALID_INPUT/);

    // Empty buffer
    assert.throws(() => validateImageBuffer(Buffer.alloc(0)), /EMPTY_IMAGE/);

    // Oversized buffer (> 5MB)
    const largeBuffer = Buffer.alloc(MAX_IMAGE_SIZE_BYTES + 10);
    assert.throws(() => validateImageBuffer(largeBuffer), /IMAGE_TOO_LARGE/);

    // Corrupted/unsupported format (random bytes)
    const randomBuffer = Buffer.from('hello this is not an image');
    assert.throws(() => validateImageBuffer(randomBuffer), /UNSUPPORTED_FORMAT/);
  });

  it('preprocesses a real test image with OpenCV (grayscale, resize, adaptive threshold, denoise)', async () => {
    const originalBuffer = fs.readFileSync(FIXTURE_PATH);

    const processed = await preprocessImage(originalBuffer, {
      grayscale: true,
      adaptiveThreshold: true,
      denoise: true,
      resizeMaxDim: 800
    });

    assert.ok(Buffer.isBuffer(processed), 'Output must be a Buffer');
    assert.ok(processed.length > 0, 'Preprocessed image buffer must not be empty');

    // Validate that processed output is still a valid PNG
    const validation = validateImageBuffer(processed);
    assert.strictEqual(validation.format, 'png');
  });

  it('gracefully fails when processing corrupted image data that has image header', async () => {
    // Fake PNG header followed by junk data
    const fakePng = Buffer.concat([
      Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
      Buffer.from('corrupted payload that cannot be decoded')
    ]);

    await assert.rejects(
      async () => {
        await preprocessImage(fakePng);
      },
      /PREPROCESSING_FAILED/
    );
  });
});
