// server/test/authAndAcademicRoutes.test.js
const { describe, it, before, after } = require('node:test');
const assert = require('node:assert');
const http = require('http');

// Enable mock mode for deterministic tests
process.env.IMS_MOCK = 'true';
process.env.NODE_ENV = 'test';

const app = require('../src/app');

describe('Auth & Academic Routes Integration Tests', () => {
  let server;
  let baseUrl;

  before(async () => {
    await new Promise((resolve) => {
      server = http.createServer(app);
      server.listen(0, '127.0.0.1', () => {
        const address = server.address();
        baseUrl = `http://127.0.0.1:${address.port}`;
        resolve();
      });
    });
  });

  after(() => {
    if (server) {
      server.close();
    }
  });

  it('GET /health returns healthy status', async () => {
    const res = await fetch(`${baseUrl}/health`);
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.status, 'healthy');
    assert.strictEqual(body.service, 'nsync-backend');
  });

  it('GET /v1/app/version returns app update metadata and download url', async () => {
    const res = await fetch(`${baseUrl}/v1/app/version`);
    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.ok(body.latestVersionName);
    assert.ok(body.downloadUrl);
  });

  it('POST /v1/auth/challenges fetches an official inline CAPTCHA challenge', async () => {
    const res = await fetch(`${baseUrl}/v1/auth/challenges`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });

    assert.strictEqual(res.status, 200);
    const body = await res.json();
    assert.strictEqual(body.success, true);
    assert.ok(body.challengeId, 'Must return a challengeId');
    assert.ok(body.captchaImage.startsWith('data:image/'), 'Must return inline data URI image');
    assert.ok(body.expiresAt > Date.now(), 'Must have valid expiration');
  });

  it('POST /v1/auth/challenges/refresh refreshes a challenge', async () => {
    // First get a challenge
    const res1 = await fetch(`${baseUrl}/v1/auth/challenges`, { method: 'POST' });
    const body1 = await res1.json();

    // Now refresh it
    const res2 = await fetch(`${baseUrl}/v1/auth/challenges/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ challengeId: body1.challengeId })
    });

    assert.strictEqual(res2.status, 200);
    const body2 = await res2.json();
    assert.strictEqual(body2.success, true);
    assert.notStrictEqual(body2.challengeId, body1.challengeId);
  });

  it('POST /v1/auth/login rejects incorrect CAPTCHA answer with 400 INVALID_CAPTCHA', async () => {
    const challengeRes = await fetch(`${baseUrl}/v1/auth/challenges`, { method: 'POST' });
    const { challengeId } = await challengeRes.json();

    const loginRes = await fetch(`${baseUrl}/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        challengeId,
        username: '2021UCO1500',
        password: 'correct_password',
        captchaAnswer: 'WRONG_CODE'
      })
    });

    assert.strictEqual(loginRes.status, 400);
    const body = await loginRes.json();
    assert.strictEqual(body.error, 'INVALID_CAPTCHA');
  });

  it('POST /v1/auth/login rejects wrong credentials with 401 INVALID_CREDENTIALS', async () => {
    const challengeRes = await fetch(`${baseUrl}/v1/auth/challenges`, { method: 'POST' });
    const { challengeId } = await challengeRes.json();

    const loginRes = await fetch(`${baseUrl}/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        challengeId,
        username: '2021UCO1500',
        password: 'wrong_password',
        captchaAnswer: '1234'
      })
    });

    assert.strictEqual(loginRes.status, 401);
    const body = await loginRes.json();
    assert.strictEqual(body.error, 'INVALID_CREDENTIALS');
  });

  it('POST /v1/auth/login succeeds with valid credentials + CAPTCHA, reuses session for attendance & timetable', async () => {
    // 1. Obtain challenge
    const challengeRes = await fetch(`${baseUrl}/v1/auth/challenges`, { method: 'POST' });
    const { challengeId } = await challengeRes.json();

    // 2. Perform login
    const loginRes = await fetch(`${baseUrl}/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        challengeId,
        username: '2021UCO1555',
        password: 'my_secret_password',
        captchaAnswer: '1234'
      })
    });

    assert.strictEqual(loginRes.status, 200);
    const loginData = await loginRes.json();
    assert.strictEqual(loginData.success, true);
    assert.ok(loginData.token, 'Must return bearer token');
    assert.strictEqual(loginData.user.rollNumber, '2021UCO1555');
    // Ensure no IMS cookies leaked
    assert.strictEqual(loginData.cookies, undefined);
    assert.strictEqual(loginData.headers, undefined);

    const authToken = loginData.token;

    // 3. Query attendance without re-authenticating (session reuse)
    const attRes = await fetch(`${baseUrl}/v1/academic/attendance`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });

    assert.strictEqual(attRes.status, 200);
    const attData = await attRes.json();
    assert.strictEqual(attData.success, true);
    assert.strictEqual(attData.userId, '2021UCO1555');
    assert.ok(attData.data.overallPercentage > 0);
    assert.ok(attData.data.subjects.length > 0);

    // 4. Query timetable without re-authenticating (session reuse)
    const ttRes = await fetch(`${baseUrl}/v1/academic/timetable`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });

    assert.strictEqual(ttRes.status, 200);
    const ttData = await ttRes.json();
    assert.strictEqual(ttData.success, true);
    assert.ok(ttData.data.periods.length > 0);

    // 5. Explicitly renew session
    const renewRes = await fetch(`${baseUrl}/v1/auth/renew`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` }
    });
    assert.strictEqual(renewRes.status, 200);
    const renewData = await renewRes.json();
    assert.strictEqual(renewData.success, true);

    // 6. Logout and verify token is revoked
    const logoutRes = await fetch(`${baseUrl}/v1/auth/logout`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authToken}` }
    });
    assert.strictEqual(logoutRes.status, 200);

    // 7. Verify subsequent requests are rejected with 401
    const postLogoutRes = await fetch(`${baseUrl}/v1/academic/attendance`, {
      headers: { Authorization: `Bearer ${authToken}` }
    });
    assert.strictEqual(postLogoutRes.status, 401);
  });
});
