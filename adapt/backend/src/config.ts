import dotenv from 'dotenv';

dotenv.config();

export const config = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '3001', 10),
  apiBaseUrl: process.env.API_BASE_URL || 'http://localhost:3001',
  frontendUrl: process.env.FRONTEND_URL || 'http://localhost:3000',

  db: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    database: process.env.DB_NAME || 'adapt_db',
    user: process.env.DB_USER || 'adapt_user',
    password: process.env.DB_PASSWORD || 'adapt_password',
  },

  jwt: {
    secret: process.env.JWT_SECRET || 'change-this-secret-in-production',
    expiry: process.env.JWT_EXPIRY || '7d',
  },

  engineService: {
    url: process.env.ENGINE_SERVICE_URL || 'http://localhost:4001',
  },
};
