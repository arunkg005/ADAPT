import express, { Express, Request, Response } from 'express';
import cors from 'cors';
import type { CorsOptions } from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { config } from './config.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import apiRoutes from './routes/index.js';
import { engineService } from './services/engineService.js';

const app: Express = express();

app.disable('x-powered-by');

// Middleware
app.use(
  helmet({
    contentSecurityPolicy: false,
    crossOriginResourcePolicy: { policy: 'cross-origin' },
  })
);

app.use(express.json({ limit: config.security.jsonBodyLimit }));

const globalLimiter = rateLimit({
  windowMs: config.security.rateLimitWindowMs,
  max: config.security.rateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests. Please try again later.' },
});

const authLimiter = rateLimit({
  windowMs: config.security.rateLimitWindowMs,
  max: config.security.authRateLimitMax,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many authentication attempts. Please try again later.' },
});

app.use(globalLimiter);
const allowedOrigins = [
  config.frontendUrl,
  ...(process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',').map((item) => item.trim()) : []),
].filter(Boolean);

const allowVercelPreviewOrigins = process.env.ALLOW_VERCEL_PREVIEWS === 'true';
const vercelFrontendPattern = /^https:\/\/frontend-[a-z0-9-]+\.vercel\.app$/i;

const isOriginAllowed = (origin?: string): boolean => {
  if (!origin) {
    return true;
  }

  if (allowedOrigins.includes(origin)) {
    return true;
  }

  if (allowVercelPreviewOrigins && vercelFrontendPattern.test(origin)) {
    return true;
  }

  return false;
};

const corsOptions: CorsOptions = {
  origin: (origin, callback) => {
    if (isOriginAllowed(origin)) {
      callback(null, true);
      return;
    }

    callback(new Error('CORS origin not allowed'));
  },
  credentials: true,
};

app.use(cors(corsOptions));
app.use('/api/auth', authLimiter);

// Health check
app.get('/health', async (req: Request, res: Response) => {
  const engineHealth = await engineService.health();
  const engineStatus = String(engineHealth?.status || '').toUpperCase();
  const engineAvailable = engineStatus === 'OK' || engineStatus === 'HEALTHY';

  res.status(engineAvailable ? 200 : 503).json({
    status: engineAvailable ? 'OK' : 'DEGRADED',
    service: 'ADAPT Backend',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    dependencies: {
      engine: engineHealth,
    },
  });
});

// Root documentation
app.get('/', (req: Request, res: Response) => {
  res.json({
    title: 'ADAPT Backend API',
    description: 'Cognitive assistance platform for elderly and cognitively challenged individuals',
    version: '1.0.0',
    endpoints: {
      health: 'GET /health',
      apiDocs: 'GET /api/docs',
      auth: 'POST /api/auth/login',
      patients: 'GET /api/patients',
      caregivers: 'GET /api/caregivers',
      devices: 'GET /api/devices',
      alerts: 'GET /api/alerts',
      telemetry: 'POST /api/telemetry',
      ai: 'POST /api/ai/chat',
      taskLab: 'GET /api/task-lab/templates',
      analysis: 'GET /api/analysis/overview',
      cognitive: 'POST /api/cognitive/assist-mode/next-action',
    },
    documentation: 'See /api/docs for full API reference',
  });
});

// API routes
app.use('/api', apiRoutes);

// Error handling
app.use(notFoundHandler);
app.use(errorHandler);

// Start server
const PORT = config.port;
app.listen(PORT, () => {
  console.log(`
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║           🧠 ADAPT Backend Server Started 🧠              ║
║                                                            ║
║  Server:   http://localhost:${PORT}                       
║  Docs:     http://localhost:${PORT}/api/docs              
║  Health:   http://localhost:${PORT}/health                
║                                                            ║
║  Environment: ${config.env}
║  Database: ${
    config.db.url
      ? 'DATABASE_URL'
      : `${config.db.host}:${config.db.port}/${config.db.database}`
  }
║  Engine:   ${config.engineService.url}
║                                                            ║
╚════════════════════════════════════════════════════════════╝
  `);
});
