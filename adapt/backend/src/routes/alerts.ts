import { Router, Response } from 'express';
import { alertService } from '../services/alertService.js';
import { caregiverService } from '../services/caregiverService.js';
import { authService } from '../services/authService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

// GET /api/alerts
router.get('/', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
    const offset = parseInt(req.query.offset as string) || 0;

    const alerts = await alertService.getAll(limit, offset);
    const total = await alertService.getUnacknowledgedCount();

    res.json({ data: alerts, total, limit, offset });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/alerts/:id
router.get('/:id', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const alert = await alertService.getById(req.params.id);
    res.json(alert);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/alerts
router.post('/', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: any, res: Response) => {
  try {
    const {
      patient_id,
      task_name,
      task_state,
      severity,
      primary_issue,
      assistance_mode,
      engine_output,
    } = req.body;

    if (!patient_id || !severity) {
      return res.status(400).json({ error: 'patient_id and severity are required' });
    }

    const alert = await alertService.create({
      patient_id,
      task_name,
      task_state,
      severity,
      primary_issue,
      assistance_mode,
      engine_output,
    });

    res.status(201).json(alert);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// PUT /api/alerts/:id/acknowledge
router.put(
  '/:id/acknowledge',
  authMiddleware,
  requireRoles('CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
  try {
    if (!req.userId) {
      return res.status(401).json({ error: 'User not authenticated' });
    }

    const user = await authService.getUserById(req.userId);
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    let caregiver = await caregiverService.getByUserId(user.id);

    if (!caregiver) {
      if (user.role !== 'CAREGIVER') {
        return res.status(403).json({ error: 'Only caregivers can acknowledge alerts' });
      }

      caregiver = await caregiverService.ensureForUser(user);
    }

    const alert = await alertService.acknowledge(req.params.id, caregiver.id);
    res.json(alert);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// DELETE /api/alerts/:id
router.delete('/:id', authMiddleware, requireRoles('ADMIN'), async (req: AuthRequest, res: Response) => {
  try {
    await alertService.delete(req.params.id);
    res.json({ message: 'Alert deleted successfully' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
