import { Router, Response } from 'express';
import authRoutes from './auth.js';
import patientRoutes from './patients.js';
import caregiverRoutes from './caregivers.js';
import deviceRoutes from './devices.js';
import alertRoutes from './alerts.js';
import telemetryRoutes from './telemetry.js';
import aiRoutes from './ai.js';
import analysisRoutes from './analysis.js';
import taskLabRoutes from './taskLab.js';
import cognitiveRoutes from './cognitive.js';

const router = Router();

// Mount route modules
router.use('/auth', authRoutes);
router.use('/patients', patientRoutes);
router.use('/caregivers', caregiverRoutes);
router.use('/devices', deviceRoutes);
router.use('/alerts', alertRoutes);
router.use('/telemetry', telemetryRoutes);
router.use('/ai', aiRoutes);
router.use('/analysis', analysisRoutes);
router.use('/task-lab', taskLabRoutes);
router.use('/cognitive', cognitiveRoutes);

// API documentation
router.get('/docs', (req: any, res: Response) => {
  const docs = {
    title: 'ADAPT Backend API',
    version: '1.0.0',
    baseUrl: 'http://localhost:3001/api',
    endpoints: {
      auth: [
        {
          method: 'POST',
          path: '/auth/login',
          description: 'User login (returns JWT token)',
          roles: ['PUBLIC'],
        },
        {
          method: 'POST',
          path: '/auth/social-login',
          description: 'Social sign-in bridge (Google/Facebook; token verification should be enforced in production)',
          roles: ['PUBLIC'],
        },
        {
          method: 'POST',
          path: '/auth/logout',
          description: 'User logout (requires Authorization header)',
          roles: ['ADMIN', 'CAREGIVER', 'PATIENT'],
        },
        {
          method: 'GET',
          path: '/auth/verify',
          description: 'Verify token validity',
          roles: ['ADMIN', 'CAREGIVER', 'PATIENT'],
        },
        {
          method: 'POST',
          path: '/auth/register',
          description: 'Public signup (supports CAREGIVER or PATIENT)',
          roles: ['PUBLIC'],
        },
        {
          method: 'POST',
          path: '/auth/register/admin',
          description: 'Admin-provisioned signup (supports ADMIN, CAREGIVER, PATIENT)',
          roles: ['ADMIN'],
        },
      ],
      patients: [
        {
          method: 'GET',
          path: '/patients',
          description: 'List all patients (paginated)',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/patients/:id',
          description: 'Get patient details',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/patients',
          description: 'Create new patient',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'PUT',
          path: '/patients/:id',
          description: 'Update patient',
          roles: ['ADMIN'],
        },
        {
          method: 'DELETE',
          path: '/patients/:id',
          description: 'Delete patient',
          roles: ['ADMIN'],
        },
        {
          method: 'GET',
          path: '/patients/:id/caregivers',
          description: 'Get caregivers for patient',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/patients/:patientId/caregivers/:caregiverId',
          description: 'Assign caregiver to patient',
          roles: ['ADMIN'],
        },
        {
          method: 'DELETE',
          path: '/patients/:patientId/caregivers/:caregiverId',
          description: 'Unassign caregiver from patient',
          roles: ['ADMIN'],
        },
      ],
      caregivers: [
        {
          method: 'GET',
          path: '/caregivers',
          description: 'List all caregivers',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/caregivers/:id',
          description: 'Get caregiver details',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/caregivers',
          description: 'Create new caregiver',
          roles: ['ADMIN'],
        },
        {
          method: 'PUT',
          path: '/caregivers/:id',
          description: 'Update caregiver',
          roles: ['ADMIN'],
        },
        {
          method: 'DELETE',
          path: '/caregivers/:id',
          description: 'Delete caregiver',
          roles: ['ADMIN'],
        },
      ],
      devices: [
        {
          method: 'GET',
          path: '/devices',
          description: 'List all devices',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/devices/:id',
          description: 'Get device details',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/devices',
          description: 'Register new device',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'PUT',
          path: '/devices/:id',
          description: 'Update device',
          roles: ['ADMIN'],
        },
        {
          method: 'PATCH',
          path: '/devices/:id/status',
          description: 'Update device online status',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'DELETE',
          path: '/devices/:id',
          description: 'Delete device',
          roles: ['ADMIN'],
        },
      ],
      alerts: [
        {
          method: 'GET',
          path: '/alerts',
          description: 'List all alerts (paginated)',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/alerts/:id',
          description: 'Get alert details',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/alerts',
          description: 'Create new alert',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'PUT',
          path: '/alerts/:id/acknowledge',
          description: 'Acknowledge alert',
          roles: ['CAREGIVER'],
        },
        {
          method: 'DELETE',
          path: '/alerts/:id',
          description: 'Delete alert',
          roles: ['ADMIN'],
        },
      ],
      telemetry: [
        {
          method: 'POST',
          path: '/telemetry',
          description: 'Submit telemetry signals',
          roles: ['ADMIN', 'CAREGIVER', 'PATIENT'],
        },
        {
          method: 'GET',
          path: '/telemetry/:patientId',
          description: 'Get patient telemetry history',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/telemetry/device/:deviceId',
          description: 'Get device telemetry history',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/telemetry/evaluate/:patientId',
          description: 'Manually trigger engine evaluation',
          roles: ['ADMIN', 'CAREGIVER'],
        },
      ],
      ai: [
        {
          method: 'POST',
          path: '/ai/chat',
          description: 'Caregiver AI guidance chat endpoint',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/ai/task-lab/generate',
          description: 'Generate Task Lab draft from AI prompt',
          roles: ['ADMIN', 'CAREGIVER'],
        },
      ],
      analysis: [
        {
          method: 'GET',
          path: '/analysis/overview',
          description: 'Get caregiver dashboard analysis overview',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/analysis/patient/:patientId/summary',
          description: 'Get patient-centric analysis summary',
          roles: ['ADMIN', 'CAREGIVER'],
        },
      ],
      taskLab: [
        {
          method: 'GET',
          path: '/task-lab/templates',
          description: 'List predefined sophisticated Task Lab templates',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'GET',
          path: '/task-lab/plans?patientId=<id>',
          description: 'Get patient Task Lab plans',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/task-lab/plans',
          description: 'Create Task Lab plan (manual/AI/template draft)',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'POST',
          path: '/task-lab/plans/from-template',
          description: 'Create Task Lab plan from predefined template',
          roles: ['ADMIN', 'CAREGIVER'],
        },
        {
          method: 'PUT',
          path: '/task-lab/plans/:id/status',
          description: 'Update plan lifecycle status (DRAFT/PUBLISHED/ARCHIVED)',
          roles: ['ADMIN', 'CAREGIVER'],
        },
      ],
      cognitive: [
        {
          method: 'POST',
          path: '/cognitive/evaluate',
          description: 'Run cognitive engine evaluation for a supplied signal snapshot',
          roles: ['ADMIN', 'CAREGIVER', 'PATIENT'],
        },
        {
          method: 'POST',
          path: '/cognitive/assist-mode/next-action',
          description: 'Get next assist-mode recommendation from cognitive engine',
          roles: ['ADMIN', 'CAREGIVER', 'PATIENT'],
        },
      ],
    },
  };

  res.json(docs);
});

export default router;
