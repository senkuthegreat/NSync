// server/src/ims/sessionStore.js
// Secure in-memory session manager with user isolation, TTL, and cookie protection.

const crypto = require('crypto');
const logger = require('../utils/logger');

const DEFAULT_SESSION_TTL_MS = 30 * 60 * 1000; // 30 minutes

class SessionStore {
  constructor(defaultTtlMs = DEFAULT_SESSION_TTL_MS) {
    this.defaultTtlMs = defaultTtlMs;
    // Map of token -> Session
    this.sessions = new Map();
    // Map of userId -> Set of tokens (for user isolation and session management)
    this.userSessions = new Map();

    // Periodic cleanup every 5 minutes
    this.cleanupTimer = setInterval(() => {
      this.cleanupExpired();
    }, 5 * 60 * 1000);

    if (this.cleanupTimer.unref) {
      this.cleanupTimer.unref();
    }
  }

  /**
   * Creates an authenticated session for a user.
   * Stores cookies internally; returns ONLY public token and metadata.
   */
  createSession({ userId, cookies, metadata = {}, ttlMs = this.defaultTtlMs }) {
    if (!userId) {
      throw new Error('userId is required to create a session');
    }
    if (!cookies) {
      throw new Error('cookies are required to store an authorized session');
    }

    const token = crypto.randomUUID();
    const now = Date.now();
    const expiresAt = now + ttlMs;

    const session = {
      token,
      userId,
      cookies, // SECURE: kept in server memory only
      metadata,
      createdAt: now,
      lastAccessedAt: now,
      expiresAt
    };

    this.sessions.set(token, session);

    if (!this.userSessions.has(userId)) {
      this.userSessions.set(userId, new Set());
    }
    this.userSessions.get(userId).add(token);

    logger.info(`Created authorized session for user ${userId}`, {
      userId,
      expiresAt: new Date(expiresAt).toISOString()
    });

    // Return sanitized view (COOKIES EXCLUDED)
    return {
      token,
      userId,
      metadata,
      createdAt: session.createdAt,
      expiresAt: session.expiresAt
    };
  }

  /**
   * Retrieves an internal session including cookies for backend operations.
   * Revalidates TTL and updates lastAccessedAt.
   */
  getSession(token) {
    if (!token) return null;
    const session = this.sessions.get(token);
    if (!session) return null;

    const now = Date.now();
    if (session.expiresAt <= now) {
      this.revokeSession(token);
      return null;
    }

    // Touch session & extend TTL
    session.lastAccessedAt = now;
    session.expiresAt = now + this.defaultTtlMs;
    return session;
  }

  /**
   * Validates a session token for client-facing middleware.
   * Does NOT return raw cookies to avoid accidental leakage.
   */
  validateSession(token) {
    const session = this.getSession(token);
    if (!session) {
      return { valid: false };
    }
    return {
      valid: true,
      userId: session.userId,
      metadata: session.metadata,
      expiresAt: session.expiresAt
    };
  }

  /**
   * Renews session expiration time.
   */
  renewSession(token, ttlMs = this.defaultTtlMs) {
    const session = this.sessions.get(token);
    if (!session) return null;

    const now = Date.now();
    if (session.expiresAt <= now) {
      this.revokeSession(token);
      return null;
    }

    session.lastAccessedAt = now;
    session.expiresAt = now + ttlMs;

    return {
      token,
      userId: session.userId,
      expiresAt: session.expiresAt
    };
  }

  /**
   * Revokes an existing session token.
   */
  revokeSession(token) {
    const session = this.sessions.get(token);
    if (!session) return false;

    this.sessions.delete(token);
    const userTokens = this.userSessions.get(session.userId);
    if (userTokens) {
      userTokens.delete(token);
      if (userTokens.size === 0) {
        this.userSessions.delete(session.userId);
      }
    }

    logger.info(`Revoked session for user ${session.userId}`);
    return true;
  }

  /**
   * Revokes all active sessions for a specific user (e.g., password change/logout-all).
   */
  revokeUserSessions(userId) {
    const tokens = this.userSessions.get(userId);
    if (!tokens) return 0;

    let count = 0;
    for (const token of Array.from(tokens)) {
      if (this.revokeSession(token)) {
        count++;
      }
    }
    return count;
  }

  /**
   * Cleans up expired sessions.
   */
  cleanupExpired() {
    const now = Date.now();
    let cleaned = 0;
    for (const [token, session] of this.sessions.entries()) {
      if (session.expiresAt <= now) {
        this.revokeSession(token);
        cleaned++;
      }
    }
    if (cleaned > 0) {
      logger.debug(`Cleaned up ${cleaned} expired sessions`);
    }
    return cleaned;
  }

  getActiveSessionCount() {
    return this.sessions.size;
  }

  destroy() {
    if (this.cleanupTimer) {
      clearInterval(this.cleanupTimer);
    }
    this.sessions.clear();
    this.userSessions.clear();
  }
}

// Export singleton instance as well as Class for testing
const defaultStore = new SessionStore();
module.exports = defaultStore;
module.exports.SessionStore = SessionStore;
