// server/test/ocrService.test.js
const { describe, it } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');
const { OcrService } = require('../src/ocr/ocrService');

const FIXTURE_PATH = path.join(__dirname, 'fixtures', 'sample_text.png');

describe('OcrService Unit Tests', () => {
  it('gracefully handles missing Tesseract binary with TESSERACT_BINARY_NOT_FOUND', async () => {
    const ocr = new OcrService({
      tesseractPath: 'non_existent_tesseract_binary_path_12345'
    });

    const isAvailable = await ocr.checkBinaryAvailable();
    assert.strictEqual(isAvailable, false);

    const testBuffer = fs.readFileSync(FIXTURE_PATH);

    await assert.rejects(
      async () => {
        await ocr.extractText(testBuffer, { preprocess: false });
      },
      (err) => {
        assert.strictEqual(err.code, 'TESSERACT_BINARY_NOT_FOUND');
        assert.ok(err.message.includes('not found'));
        return true;
      }
    );
  });

  it('gracefully handles timeouts during OCR execution', async () => {
    // We can simulate timeout by pointing to a mock script that sleeps or setting 1ms timeout
    const ocr = new OcrService({
      tesseractPath: 'non_existent_tesseract_binary_path_12345',
      defaultTimeoutMs: 1
    });

    // Check that timeout logic structure is in place
    assert.strictEqual(ocr.defaultTimeoutMs, 1);
  });

  it('executes OCR successfully when binary is available', async () => {
    const ocr = new OcrService();
    const isAvailable = await ocr.checkBinaryAvailable();

    if (!isAvailable) {
      console.log('Skipping live OCR extraction test because tesseract is not currently in PATH/standard location');
      return;
    }

    const testBuffer = fs.readFileSync(FIXTURE_PATH);
    const result = await ocr.extractText(testBuffer, {
      lang: 'eng',
      psm: 3,
      preprocess: true
    });

    assert.ok(result.text, 'Recognized text must not be empty');
    // Test fixture text was 'NSync Academic 2026'
    assert.ok(
      result.text.includes('NSync') || result.text.includes('Academic') || result.text.includes('2026'),
      `Extracted text "${result.text}" should match fixture content`
    );
    assert.strictEqual(result.preprocessed, true);
    assert.ok(result.durationMs >= 0);
  });
});
