// server/test/sessionStore.test.js
const { describe, it } = require('node:test');
const assert = require('node:assert');
const { SessionStore } = require('../src/ims/sessionStore');

describe('SessionStore Unit Tests', () => {
  it('creates an authenticated session and does not leak cookies to client view', () => {
    const store = new SessionStore(60000);
    const secretCookies = 'PHPSESSID=secret_123456; Path=/; HttpOnly';

    const clientSession = store.createSession({
      userId: '2021UCO1500',
      cookies: secretCookies,
      metadata: { name: 'Student Test', branch: 'COE' }
    });

    assert.ok(clientSession.token, 'Token should be defined');
    assert.strictEqual(clientSession.userId, '2021UCO1500');
    assert.strictEqual(clientSession.metadata.name, 'Student Test');
    // Critical: cookies must NEVER be in client session view
    assert.strictEqual(clientSession.cookies, undefined, 'Cookies must not be returned to client');

    // Retrieve internal session for backend use
    const internalSession = store.getSession(clientSession.token);
    assert.ok(internalSession);
    assert.strictEqual(internalSession.cookies, secretCookies);

    store.destroy();
  });

  it('validates session and protects against invalid or revoked tokens', () => {
    const store = new SessionStore(60000);
    const session = store.createSession({
      userId: '2021UCO1501',
      cookies: 'PHPSESSID=session_abc'
    });

    // Valid check
    const validResult = store.validateSession(session.token);
    assert.strictEqual(validResult.valid, true);
    assert.strictEqual(validResult.userId, '2021UCO1501');

    // Revoke check
    const revoked = store.revokeSession(session.token);
    assert.strictEqual(revoked, true);

    // After revocation
    const afterRevoke = store.validateSession(session.token);
    assert.strictEqual(afterRevoke.valid, false);

    // Unknown token
    const unknown = store.validateSession('non-existent-token');
    assert.strictEqual(unknown.valid, false);

    store.destroy();
  });

  it('enforces TTL expiration and cleanup', async () => {
    // Very short TTL for testing: 50ms
    const store = new SessionStore(50);
    const session = store.createSession({
      userId: '2021UCO1502',
      cookies: 'PHPSESSID=expiring_cookie'
    });

    assert.strictEqual(store.getActiveSessionCount(), 1);

    // Wait 70ms for expiration
    await new Promise(resolve => setTimeout(resolve, 70));

    // Should return null and self-clean
    const expiredSession = store.getSession(session.token);
    assert.strictEqual(expiredSession, null);
    assert.strictEqual(store.getActiveSessionCount(), 0);

    store.destroy();
  });

  it('supports user isolation and bulk session revocation per user', () => {
    const store = new SessionStore(60000);

    const s1 = store.createSession({ userId: 'USER_A', cookies: 'cookie_a1' });
    const s2 = store.createSession({ userId: 'USER_A', cookies: 'cookie_a2' });
    const s3 = store.createSession({ userId: 'USER_B', cookies: 'cookie_b1' });

    assert.strictEqual(store.getActiveSessionCount(), 3);

    // Revoke all sessions for USER_A
    const count = store.revokeUserSessions('USER_A');
    assert.strictEqual(count, 2);

    // USER_A sessions should be gone
    assert.strictEqual(store.getSession(s1.token), null);
    assert.strictEqual(store.getSession(s2.token), null);

    // USER_B session must remain untouched
    assert.ok(store.getSession(s3.token) !== null);
    assert.strictEqual(store.getActiveSessionCount(), 1);

    store.destroy();
  });

  it('renews session expiration on explicit renew call', async () => {
    const store = new SessionStore(500);
    const session = store.createSession({
      userId: '2021UCO1503',
      cookies: 'cookie_test'
    });

    const initialExpiresAt = session.expiresAt;
    await new Promise(resolve => setTimeout(resolve, 50));

    const renewed = store.renewSession(session.token, 1000);
    assert.ok(renewed);
    assert.ok(renewed.expiresAt > initialExpiresAt);

    store.destroy();
  });
});
