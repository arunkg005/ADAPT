import { Router, Response } from 'express';
import { taskLabService } from '../services/taskLabService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

router.get('/templates', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), (req: AuthRequest, res: Response) => {
  res.json({ data: taskLabService.getTemplates() });
});

router.get('/plans', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const patientId = String(req.query.patientId || '').trim();
    if (!patientId) {
      return res.status(400).json({ error: 'patientId query parameter is required' });
    }

    const limit = Math.min(parseInt(String(req.query.limit || '50'), 10) || 50, 200);
    const offset = parseInt(String(req.query.offset || '0'), 10) || 0;
    const status = req.query.status ? String(req.query.status) : undefined;

    const plans = await taskLabService.getByPatientId(patientId, limit, offset, status);
    const total = await taskLabService.getPatientPlanCount(patientId);

    res.json({ data: plans, total, limit, offset });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to load task plans' });
  }
});

router.get('/plans/:id', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const plan = await taskLabService.getById(req.params.id);
    res.json(plan);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to load task plan' });
  }
});

router.post('/plans', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const payload = req.body || {};
    const created = await taskLabService.create({
      patient_id: String(payload.patient_id || ''),
      created_by_user_id: req.userId,
      title: String(payload.title || ''),
      description: payload.description ? String(payload.description) : undefined,
      scheduled_time: payload.scheduled_time ? String(payload.scheduled_time) : undefined,
      task_type: payload.task_type ? String(payload.task_type) : undefined,
      risk_level: payload.risk_level ? String(payload.risk_level) : undefined,
      complexity: payload.complexity ? String(payload.complexity) : undefined,
      status: payload.status ? String(payload.status) : undefined,
      source: payload.source ? String(payload.source) : undefined,
      template_key: payload.template_key ? String(payload.template_key) : undefined,
      steps: Array.isArray(payload.steps) ? payload.steps : undefined,
    });

    res.status(201).json(created);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to create task plan' });
  }
});

router.post('/plans/from-template', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const { patient_id, template_key, scheduled_time, risk_level } = req.body || {};

    if (!patient_id || !template_key) {
      return res.status(400).json({ error: 'patient_id and template_key are required' });
    }

    const plan = await taskLabService.createFromTemplate({
      patient_id: String(patient_id),
      created_by_user_id: req.userId,
      template_key: String(template_key),
      scheduled_time: scheduled_time ? String(scheduled_time) : undefined,
      risk_level: risk_level ? String(risk_level) : undefined,
    });

    res.status(201).json(plan);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to create plan from template' });
  }
});

router.put('/plans/:id/status', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const status = String(req.body?.status || '').trim();
    if (!status) {
      return res.status(400).json({ error: 'status is required' });
    }

    const updated = await taskLabService.updateStatus(req.params.id, status);
    res.json(updated);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to update plan status' });
  }
});

router.put(
  '/plans/:planId/steps/:stepId/completion',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
    try {
      const isCompleted = Boolean(req.body?.is_completed);
      const updated = await taskLabService.updateStepCompletion(req.params.planId, req.params.stepId, isCompleted);
      res.json(updated);
    } catch (error: any) {
      res.status(error.status || 500).json({ error: error.message || 'Unable to update step completion' });
    }
  }
);

export default router;
