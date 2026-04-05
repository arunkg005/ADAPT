import { Router, Response } from 'express';
import authRoutes from './auth.js';
import patientRoutes from './patients.js';
import caregiverRoutes from './caregivers.js';
import deviceRoutes from './devices.js';
import alertRoutes from './alerts.js';
import telemetryRoutes from './telemetry.js';

const router = Router();

// Mount route modules
router.use('/auth', authRoutes);
router.use('/patients', patientRoutes);
router.use('/caregivers', caregiverRoutes);
router.use('/devices', deviceRoutes);
router.use('/alerts', alertRoutes);
router.use('/telemetry', telemetryRoutes);

// API documentation
router.get('/docs', (req: any, res: Response) => {
  const docs = {
    title: 'ADAPT Backend API',
    version: '1.0.0',
    baseUrl: 'http://localhost:3001/api',
    endpoints: {
      auth: [
        { method: 'POST', path: '/auth/login', description: 'User login (returns JWT token)' },
        {
          method: 'POST',
          path: '/auth/logout',
          description: 'User logout (requires Authorization header)',
        },
        { method: 'GET', path: '/auth/verify', description: 'Verify token validity' },
        { method: 'POST', path: '/auth/register', description: 'Create new user (admin only)' },
      ],
      patients: [
        { method: 'GET', path: '/patients', description: 'List all patients (paginated)' },
        { method: 'GET', path: '/patients/:id', description: 'Get patient details' },
        { method: 'POST', path: '/patients', description: 'Create new patient' },
        { method: 'PUT', path: '/patients/:id', description: 'Update patient' },
        { method: 'DELETE', path: '/patients/:id', description: 'Delete patient' },
        { method: 'GET', path: '/patients/:id/caregivers', description: 'Get caregivers for patient' },
        {
          method: 'POST',
          path: '/patients/:patientId/caregivers/:caregiverId',
          description: 'Assign caregiver to patient',
        },
      ],
      caregivers: [
        { method: 'GET', path: '/caregivers', description: 'List all caregivers' },
        { method: 'GET', path: '/caregivers/:id', description: 'Get caregiver details' },
        { method: 'POST', path: '/caregivers', description: 'Create new caregiver' },
        { method: 'PUT', path: '/caregivers/:id', description: 'Update caregiver' },
        { method: 'DELETE', path: '/caregivers/:id', description: 'Delete caregiver' },
      ],
      devices: [
        { method: 'GET', path: '/devices', description: 'List all devices' },
        { method: 'GET', path: '/devices/:id', description: 'Get device details' },
        { method: 'POST', path: '/devices', description: 'Register new device' },
        { method: 'PUT', path: '/devices/:id', description: 'Update device' },
        { method: 'PATCH', path: '/devices/:id/status', description: 'Update device online status' },
        { method: 'DELETE', path: '/devices/:id', description: 'Delete device' },
      ],
      alerts: [
        { method: 'GET', path: '/alerts', description: 'List all alerts (paginated)' },
        { method: 'GET', path: '/alerts/:id', description: 'Get alert details' },
        { method: 'POST', path: '/alerts', description: 'Create new alert' },
        { method: 'PUT', path: '/alerts/:id/acknowledge', description: 'Acknowledge alert' },
        { method: 'DELETE', path: '/alerts/:id', description: 'Delete alert' },
      ],
      telemetry: [
        { method: 'POST', path: '/telemetry', description: 'Submit telemetry signals' },
        { method: 'GET', path: '/telemetry/:patientId', description: 'Get patient telemetry history' },
        {
          method: 'POST',
          path: '/telemetry/evaluate/:patientId',
          description: 'Manually trigger engine evaluation',
        },
      ],
    },
  };

  res.json(docs);
});

export default router;
