import dotenv from 'dotenv';

dotenv.config();

const env = process.env.NODE_ENV || 'development';
const isProduction = env === 'production';
const databaseUrl = process.env.DATABASE_URL?.trim();

const getRequiredOrDefault = (
  key: string,
  fallback: string,
  options: { requiredInProduction?: boolean } = {}
): string => {
  const value = process.env[key]?.trim();
  if (value) {
    return value;
  }

  if (isProduction && options.requiredInProduction) {
    throw new Error(`Missing required environment variable: ${key}`);
  }

  return fallback;
};

const jwtSecret = getRequiredOrDefault('JWT_SECRET', 'change-this-secret-in-development', {
  requiredInProduction: true,
});

if (isProduction && !databaseUrl && !process.env.DB_PASSWORD?.trim()) {
  throw new Error('Missing required environment variable: DB_PASSWORD (or set DATABASE_URL)');
}

if (isProduction && jwtSecret.length < 32) {
  throw new Error('JWT_SECRET must be at least 32 characters in production');
}

export const config = {
  env,
  port: parseInt(process.env.PORT || '3001', 10),
  apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:3001',
  frontendUrl: process.env.FRONTEND_URL || 'http://localhost:3000',

  security: {
    jsonBodyLimit: process.env.REQUEST_BODY_LIMIT || '1mb',
    rateLimitWindowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000', 10),
    rateLimitMax: parseInt(process.env.RATE_LIMIT_MAX || '600', 10),
    authRateLimitMax: parseInt(process.env.AUTH_RATE_LIMIT_MAX || '40', 10),
  },

  db: {
    url: databaseUrl,
    ssl: process.env.DB_SSL === 'true',
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    database: process.env.DB_NAME || 'adapt_db',
    user: process.env.DB_USER || 'adapt_user',
    password: databaseUrl ? process.env.DB_PASSWORD || '' : getRequiredOrDefault('DB_PASSWORD', 'adapt_password'),
  },

  jwt: {
    secret: jwtSecret,
    expiry: process.env.JWT_EXPIRY || '7d',
  },

  engineService: {
    url:
      process.env.ENGINE_SERVICE_URL ||
      (process.env.ENGINE_SERVICE_HOST
        ? `http://${process.env.ENGINE_SERVICE_HOST}${
            process.env.ENGINE_SERVICE_PORT ? `:${process.env.ENGINE_SERVICE_PORT}` : ''
          }`
        : 'http://localhost:4001'),
  },
};
