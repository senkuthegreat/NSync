// server/src/utils/logger.js
// Safe logging utility that redacts sensitive keys and secrets.

const SENSITIVE_KEYS = new Set([
  'password',
  'pwd',
  'token',
  'authorization',
  'cookie',
  'cookies',
  'captcha',
  'captchaanswer',
  'phpsessid',
  'secret'
]);

function redact(obj, depth = 0) {
  if (depth > 6) return '[MAX_DEPTH]';
  if (!obj || typeof obj !== 'object') {
    if (typeof obj === 'string') {
      // Redact potential cookie strings or bearer tokens if embedded in strings
      return obj
        .replace(/(Bearer\s+)[A-Za-z0-9-_.]+/gi, '$1[REDACTED]')
        .replace(/(PHPSESSID=)[A-Za-z0-9]+/gi, '$1[REDACTED]');
    }
    return obj;
  }

  if (Array.isArray(obj)) {
    return obj.map(item => redact(item, depth + 1));
  }

  const sanitized = {};
  for (const [key, value] of Object.entries(obj)) {
    const lowerKey = key.toLowerCase();
    if (SENSITIVE_KEYS.has(lowerKey)) {
      sanitized[key] = '[REDACTED]';
    } else if (typeof value === 'object' && value !== null) {
      sanitized[key] = redact(value, depth + 1);
    } else if (typeof value === 'string') {
      sanitized[key] = redact(value, depth + 1);
    } else {
      sanitized[key] = value;
    }
  }
  return sanitized;
}

const logger = {
  info: (message, meta) => {
    const metaStr = meta ? ` ${JSON.stringify(redact(meta))}` : '';
    console.log(`[INFO] [${new Date().toISOString()}] ${message}${metaStr}`);
  },
  warn: (message, meta) => {
    const metaStr = meta ? ` ${JSON.stringify(redact(meta))}` : '';
    console.warn(`[WARN] [${new Date().toISOString()}] ${message}${metaStr}`);
  },
  error: (message, meta) => {
    const metaStr = meta ? ` ${JSON.stringify(redact(meta))}` : '';
    console.error(`[ERROR] [${new Date().toISOString()}] ${message}${metaStr}`);
  },
  debug: (message, meta) => {
    if (process.env.DEBUG === 'true') {
      const metaStr = meta ? ` ${JSON.stringify(redact(meta))}` : '';
      console.log(`[DEBUG] [${new Date().toISOString()}] ${message}${metaStr}`);
    }
  },
  redact
};

module.exports = logger;
