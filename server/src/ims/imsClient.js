// server/src/ims/imsClient.js
// Client for NSUT IMS portal authentication, academic services, and automated CAPTCHA solving.

const crypto = require('crypto');
const logger = require('../utils/logger');
const ocrService = require('../ocr/ocrService');

const CHALLENGE_TTL_MS = 5 * 60 * 1000; // 5 minutes

class ImsClient {
  constructor(options = {}) {
    this.baseUrl = options.baseUrl || process.env.IMS_BASE_URL || 'https://www.imsnsit.org/imsnsit';
    this.isMock = options.isMock ?? (process.env.IMS_MOCK === 'true');
    // In-memory challenge store: challengeId -> { cookies, encFy, fy, rawBuffer, createdAt, expiresAt }
    this.challenges = new Map();
  }

  /**
   * Helper to merge Set-Cookie header strings into a cookie map or cookie string.
   */
  _mergeCookies(existingCookieStr = '', setCookieHeaders = []) {
    const cookieMap = new Map();
    if (existingCookieStr) {
      existingCookieStr.split(';').forEach(part => {
        const [k, ...v] = part.trim().split('=');
        if (k) cookieMap.set(k.trim(), v.join('='));
      });
    }

    const headersList = Array.isArray(setCookieHeaders) ? setCookieHeaders : [setCookieHeaders].filter(Boolean);
    headersList.forEach(header => {
      // Split on comma if multiple cookies are concatenated
      const rawParts = header.split(/,(?=[^;]+=[^;]+)/);
      rawParts.forEach(raw => {
        const firstPart = raw.split(';')[0].trim();
        const [k, ...v] = firstPart.split('=');
        if (k && !['expires', 'path', 'domain', 'samesite', 'secure', 'httponly'].includes(k.toLowerCase())) {
          cookieMap.set(k.trim(), v.join('='));
        }
      });
    });

    return Array.from(cookieMap.entries())
      .map(([k, v]) => `${k}=${v}`)
      .join('; ');
  }

  /**
   * Generates a new official CAPTCHA challenge from the IMS portal.
   * Keeps the challenge session cookies safe on the backend.
   * Returns a challengeId and base64-encoded image to display to the student.
   */
  async fetchCaptchaChallenge() {
    const challengeId = crypto.randomUUID();
    const now = Date.now();
    const expiresAt = now + CHALLENGE_TTL_MS;

    if (this.isMock) {
      const mockImageBase64 = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
      this.challenges.set(challengeId, {
        cookies: 'PHPSESSID=mock_session_' + challengeId,
        encFy: 'mock_enc_fy',
        fy: '2026-27',
        rawBuffer: Buffer.from(mockImageBase64.replace(/^data:image\/[a-z0-9-+.]+;base64,/, ''), 'base64'),
        createdAt: now,
        expiresAt,
        mockExpectedCaptcha: '1234'
      });

      return {
        challengeId,
        captchaImage: mockImageBase64,
        expiresAt
      };
    }

    try {
      // Step 1: Request initial session from student_login110.php
      const initRes = await fetch(`${this.baseUrl}/student_login110.php`, {
        method: 'GET',
        headers: {
          'Referer': `${this.baseUrl}/student.htm`,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
      });

      let cookies = this._mergeCookies('', initRes.headers.getSetCookie ? initRes.headers.getSetCookie() : [initRes.headers.get('set-cookie')]);

      // Step 2: Request plum login page to get CAPTCHA image reference and financial year
      const plumRes = await fetch(`${this.baseUrl}/plum5_fw_login.php?uid=`, {
        method: 'GET',
        headers: {
          'Referer': `${this.baseUrl}/student_login110.php`,
          'Cookie': cookies,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }
      });

      cookies = this._mergeCookies(cookies, plumRes.headers.getSetCookie ? plumRes.headers.getSetCookie() : [plumRes.headers.get('set-cookie')]);
      const plumHtml = await plumRes.text();

      // Extract captcha image src
      const captchaMatch = plumHtml.match(/<img[^>]+(?:id="captchaimg"|src="([^"]*captcha[^"]*)")/i);
      let captchaSrc = captchaMatch ? (captchaMatch[1] || captchaMatch[0].match(/src="([^"]+)"/)?.[1]) : null;
      if (!captchaSrc) {
        captchaSrc = 'captchaimg.php';
      }

      const fullCaptchaUrl = captchaSrc.startsWith('http')
        ? captchaSrc
        : `${this.baseUrl}/${captchaSrc.replace(/^\/+/, '')}`;

      // Extract enc_fy and fy
      const encFyMatch = plumHtml.match(/name=["']enc_fy["'][^>]*value=["']([^"']*)["']/i);
      const encFy = encFyMatch ? encFyMatch[1] : null;

      const fyMatch = plumHtml.match(/name=["']fy["'][^>]*>[\s\S]*?<option[^>]*selected[^>]*value=["']([^"']*)["']/i);
      const fy = fyMatch ? fyMatch[1] : '2026-27';

      // Step 3: Fetch CAPTCHA image
      const captchaRes = await fetch(fullCaptchaUrl, {
        method: 'GET',
        headers: {
          'Referer': `${this.baseUrl}/plum5_fw_login.php?uid=`,
          'Cookie': cookies,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
        }
      });

      if (!captchaRes.ok) {
        throw new Error(`Failed to fetch CAPTCHA image: ${captchaRes.status} ${captchaRes.statusText}`);
      }

      cookies = this._mergeCookies(cookies, captchaRes.headers.getSetCookie ? captchaRes.headers.getSetCookie() : [captchaRes.headers.get('set-cookie')]);

      const arrayBuffer = await captchaRes.arrayBuffer();
      const buffer = Buffer.from(arrayBuffer);
      const mimeType = captchaRes.headers.get('content-type') || 'image/jpeg';
      const base64Image = `data:${mimeType};base64,${buffer.toString('base64')}`;

      this.challenges.set(challengeId, {
        cookies,
        encFy,
        fy,
        rawBuffer: buffer,
        createdAt: now,
        expiresAt
      });

      logger.info('Generated fresh IMS CAPTCHA challenge', { challengeId, expiresAt: new Date(expiresAt).toISOString() });

      return {
        challengeId,
        captchaImage: base64Image,
        expiresAt
      };
    } catch (err) {
      logger.error('Error fetching IMS CAPTCHA challenge', { error: err.message });
      throw new Error(`IMS_NETWORK_ERROR: ${err.message}`);
    }
  }

  /**
   * Refreshes an expired or unreadable challenge for the user.
   */
  async refreshCaptchaChallenge(existingChallengeId) {
    if (existingChallengeId) {
      this.challenges.delete(existingChallengeId);
    }
    return this.fetchCaptchaChallenge();
  }

  /**
   * Automatically solves a CAPTCHA challenge using OpenCV preprocessing and Tesseract OCR.
   */
  async solveChallenge(challengeId) {
    const challenge = this.challenges.get(challengeId);
    if (!challenge) {
      const err = new Error('CAPTCHA challenge has expired or does not exist.');
      err.code = 'CHALLENGE_EXPIRED';
      throw err;
    }

    if (this.isMock) {
      return challenge.mockExpectedCaptcha || '1234';
    }

    if (!challenge.rawBuffer) {
      throw new Error('NO_IMAGE_BUFFER: Captcha image buffer not available for solving');
    }

    try {
      // Run OpenCV preprocessing (grayscale, adaptive threshold, denoise) + Tesseract OCR
      const ocrResult = await ocrService.extractText(challenge.rawBuffer, {
        psm: 7, // Treat as a single line of text
        preprocess: true,
        preprocessOptions: {
          adaptive_threshold: true,
          denoise: true,
          min_dim_upscale: 150
        }
      });

      const solvedText = (ocrResult.text || '').replace(/[^a-zA-Z0-9]/g, '').trim();
      logger.info('Auto-solved CAPTCHA with OpenCV+Tesseract', {
        challengeId,
        solvedText
      });
      return solvedText;
    } catch (err) {
      logger.warn('Failed to auto-solve CAPTCHA via OCR', { error: err.message });
      throw err;
    }
  }

  /**
   * Automatically logs in without requiring user CAPTCHA entry.
   * Solves the challenge using OpenCV + Tesseract OCR.
   */
  async autoAuthenticate({ username, password, maxRetries = 2 }) {
    let lastError = null;
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      const challenge = await this.fetchCaptchaChallenge();
      const captchaAnswer = await this.solveChallenge(challenge.challengeId);
      try {
        const authResult = await this.authenticate({
          challengeId: challenge.challengeId,
          username,
          password,
          captchaAnswer
        });
        return authResult;
      } catch (err) {
        lastError = err;
        if (err.code === 'INVALID_CAPTCHA' && attempt < maxRetries) {
          logger.info(`CAPTCHA recognition retry ${attempt + 1}/${maxRetries}...`);
          continue;
        }
        throw err;
      }
    }
    throw lastError || new Error('Auto-authentication failed');
  }

  /**
   * Authenticates user against IMS using credentials and user's CAPTCHA answer.
   */
  async authenticate({ challengeId, username, password, captchaAnswer }) {
    if (!challengeId) {
      const err = new Error('Challenge ID is required');
      err.code = 'INVALID_CHALLENGE';
      throw err;
    }

    const challenge = this.challenges.get(challengeId);
    if (!challenge) {
      const err = new Error('CAPTCHA challenge has expired or does not exist. Please refresh the CAPTCHA.');
      err.code = 'CHALLENGE_EXPIRED';
      throw err;
    }

    if (Date.now() > challenge.expiresAt) {
      this.challenges.delete(challengeId);
      const err = new Error('CAPTCHA challenge has expired. Please refresh the CAPTCHA.');
      err.code = 'CHALLENGE_EXPIRED';
      throw err;
    }

    if (!captchaAnswer || captchaAnswer.trim() === '') {
      const err = new Error('CAPTCHA answer is required');
      err.code = 'INVALID_CAPTCHA';
      throw err;
    }

    if (!username || !password) {
      const err = new Error('Roll number and password are required');
      err.code = 'INVALID_CREDENTIALS';
      throw err;
    }

    // Handle Mock Authentication for testing
    if (this.isMock) {
      if (challenge.mockExpectedCaptcha && captchaAnswer.trim() !== challenge.mockExpectedCaptcha) {
        const err = new Error('Invalid CAPTCHA security code entered');
        err.code = 'INVALID_CAPTCHA';
        throw err;
      }

      if (password === 'wrong_password') {
        const err = new Error('Invalid Roll Number or Password');
        err.code = 'INVALID_CREDENTIALS';
        throw err;
      }

      this.challenges.delete(challengeId);
      return {
        userId: username,
        cookies: `PHPSESSID=auth_${crypto.randomUUID()}; authenticated=true`,
        metadata: {
          rollNumber: username,
          authTime: new Date().toISOString()
        }
      };
    }

    // Production IMS POST request to plum5_fw_login.php
    try {
      const formParams = new URLSearchParams();
      formParams.append('f', '');
      formParams.append('uid', username.trim().toUpperCase());
      formParams.append('pwd', password.trim());
      formParams.append('cap', captchaAnswer.trim());
      formParams.append('comp', 'NETAJI SUBHAS UNIVERSITY OF TECHNOLOGY');
      formParams.append('fy', challenge.fy || '2026-27');
      if (challenge.encFy) {
        formParams.append('enc_fy', challenge.encFy);
      }
      formParams.append('login', 'Login');

      const loginRes = await fetch(`${this.baseUrl}/plum5_fw_login.php`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Referer': `${this.baseUrl}/plum5_fw_login.php?uid=`,
          'Origin': 'https://www.imsnsit.org',
          'Cookie': challenge.cookies,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
        },
        body: formParams.toString(),
        redirect: 'follow'
      });

      const bodyText = await loginRes.text();
      const updatedCookies = this._mergeCookies(challenge.cookies, loginRes.headers.getSetCookie ? loginRes.headers.getSetCookie() : [loginRes.headers.get('set-cookie')]);

      // Clean up used challenge
      this.challenges.delete(challengeId);

      // Verify response body
      if (bodyText.includes('Invalid security Number') ||
          bodyText.includes('Security code does not match') ||
          bodyText.includes('Please Enter Captcha')) {
        const err = new Error('Invalid CAPTCHA security code entered');
        err.code = 'INVALID_CAPTCHA';
        throw err;
      }

      if (bodyText.includes('Invalid User ID') ||
          bodyText.includes('Invalid Password') ||
          bodyText.includes('Invalid Roll No') ||
          bodyText.includes('Login Failed')) {
        const err = new Error('Invalid Roll Number or Password');
        err.code = 'INVALID_CREDENTIALS';
        throw err;
      }

      logger.info('IMS authentication successful', { userId: username });

      return {
        userId: username,
        cookies: updatedCookies,
        metadata: {
          rollNumber: username,
          authTime: new Date().toISOString()
        }
      };
    } catch (err) {
      if (err.code) throw err;
      logger.error('IMS login network error', { error: err.message });
      const networkErr = new Error(`Failed to communicate with IMS: ${err.message}`);
      networkErr.code = 'IMS_NETWORK_ERROR';
      throw networkErr;
    }
  }

  /**
   * Fetches attendance reusing active authorized cookies.
   */
  async fetchAttendance(cookies) {
    if (!cookies) {
      const err = new Error('No session cookies provided');
      err.code = 'SESSION_EXPIRED';
      throw err;
    }

    if (this.isMock) {
      return {
        overallPercentage: 86.4,
        subjects: [
          { subjectCode: 'CO301', subjectName: 'Software Engineering', attended: 22, total: 25, percentage: 88.0 },
          { subjectCode: 'CO302', subjectName: 'Computer Networks', attended: 24, total: 28, percentage: 85.7 },
          { subjectCode: 'CO303', subjectName: 'Compiler Design', attended: 19, total: 22, percentage: 86.3 }
        ]
      };
    }

    try {
      const res = await fetch(`${this.baseUrl}/plum_url.php?P59Otr7+3xJIlCBBABoVb0GIgAXiKbA1LIAMUeXV5ZvLND/AfclikCFEYYIO4oq3BkrlNPPPoLsLVLt2ihg6VdTT8AXD3bdKB07WmGbjTQ4PM9NWTedZH8r2ohs9zsBsbFmAs01PXjfJn07m4ARSsaaeuZxTy4F4EJp+SoAk4dHNGvsD3yrO6W3oQUld+8626tyicw9vPYtiH/KgrqEbUsWbLNgbppb9cgWn/IrjvV15vuFRdLZIHnwX5Qndspns`, {
        method: 'GET',
        headers: {
          'Cookie': cookies,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
        }
      });

      const html = await res.text();
      if (html.includes('studentlogin.php') || html.includes('Session Expired') || html.includes('Please Login') || html.includes('plum5_fw_login.php')) {
        const err = new Error('IMS session has expired');
        err.code = 'SESSION_EXPIRED';
        throw err;
      }

      return {
        rawHtml: html,
        timestamp: new Date().toISOString()
      };
    } catch (err) {
      if (err.code) throw err;
      throw new Error(`IMS_NETWORK_ERROR: ${err.message}`);
    }
  }

  /**
   * Fetches timetable reusing active authorized cookies.
   */
  async fetchTimetable(cookies) {
    if (!cookies) {
      const err = new Error('No session cookies provided');
      err.code = 'SESSION_EXPIRED';
      throw err;
    }

    if (this.isMock) {
      return {
        day: 'Monday',
        periods: [
          { time: '09:00 - 10:00', subject: 'CO301 - Software Engineering', room: 'LH-101' },
          { time: '10:00 - 11:00', subject: 'CO302 - Computer Networks', room: 'LH-102' }
        ]
      };
    }

    try {
      const res = await fetch(`${this.baseUrl}/plum_url.php?41uZnOKcTatoQuZ1nS5qVGIuiLOuCZGGw43gNaDJuC6JU+zlhWK6ya/tcFLywtouIo7aWVZdCbDde4Onb0dyebiOoF5CwOCF9cWNylj6wkXd0RX8bVnVGaPKlMtStKmyuAZXCzEdKq+zBvagqtna+Vjw3K+2YkEhpEDLFH9RwuVsL162Qyo3TRed5amA2UTcldG13/T8xEKGB82zR7JxVA==`, {
        method: 'GET',
        headers: {
          'Cookie': cookies,
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
        }
      });

      const html = await res.text();
      if (html.includes('studentlogin.php') || html.includes('Session Expired') || html.includes('Please Login') || html.includes('plum5_fw_login.php')) {
        const err = new Error('IMS session has expired');
        err.code = 'SESSION_EXPIRED';
        throw err;
      }

      return {
        rawHtml: html,
        timestamp: new Date().toISOString()
      };
    } catch (err) {
      if (err.code) throw err;
      throw new Error(`IMS_NETWORK_ERROR: ${err.message}`);
    }
  }
}

const defaultClient = new ImsClient();
module.exports = defaultClient;
module.exports.ImsClient = ImsClient;
