import express, { Express, Request, Response } from 'express';
import cors from 'cors';
import { config } from './config.js';
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js';
import apiRoutes from './routes/index.js';
import { engineService } from './services/engineService.js';

const app: Express = express();

// Middleware
app.use(express.json());
const allowedOrigins = [
  config.frontendUrl,
  ...(process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',').map((item) => item.trim()) : []),
].filter(Boolean);

app.use(
  cors({
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.includes(origin)) {
        callback(null, true);
        return;
      }
      callback(new Error('CORS origin not allowed'));
    },
    credentials: true,
  })
);

// Health check
app.get('/health', async (req: Request, res: Response) => {
  try {
    const engineHealth = await engineService.health();
    res.json({
      status: 'OK',
      service: 'ADAPT Backend',
      version: '1.0.0',
      timestamp: new Date().toISOString(),
      engine: engineHealth,
    });
  } catch (error) {
    res.json({
      status: 'OK',
      service: 'ADAPT Backend',
      version: '1.0.0',
      timestamp: new Date().toISOString(),
      engine: { status: 'UNAVAILABLE' },
    });
  }
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
║  Database: ${config.db.host}:${config.db.port}/${config.db.database}
║  Engine:   ${config.engineService.url}
║                                                            ║
╚════════════════════════════════════════════════════════════╝
  `);
});
