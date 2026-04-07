import { Router, Response } from 'express';
import { cognitiveService } from '../services/cognitiveService.js';
import { alertService } from '../services/alertService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

const canCreateAlert = (role?: string): boolean => {
  const normalized = String(role || '').trim().toUpperCase();
  return normalized === 'ADMIN' || normalized === 'CAREGIVER';
};

router.post(
  '/evaluate',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER', 'PATIENT'),
  async (req: AuthRequest, res: Response) => {
    try {
      const engineInput = req.body?.input && typeof req.body.input === 'object' ? req.body.input : req.body;
      const evaluation = await cognitiveService.evaluate(engineInput);

      const createAlert = Boolean(req.body?.createAlert);
      let alert: any = null;

      if (
        createAlert &&
        canCreateAlert(req.userRole) &&
        String(evaluation?.severity || 'NONE').toUpperCase() !== 'NONE' &&
        engineInput?.patientId
      ) {
        alert = await alertService.create({
          patient_id: String(engineInput.patientId),
          task_name: String(engineInput.taskContext?.taskName || 'Cognitive Evaluation'),
          task_state: String(evaluation.taskState || 'UNKNOWN'),
          severity: String(evaluation.severity || 'MODERATE'),
          primary_issue: String(evaluation.primaryIssue || 'NONE'),
          assistance_mode: String(evaluation.assistanceMode || 'NONE'),
          engine_output: evaluation,
        });
      }

      res.json({
        evaluation,
        alertCreated: Boolean(alert),
        alert,
      });
    } catch (error: any) {
      res.status(error.status || 500).json({ error: error.message || 'Cognitive evaluation failed' });
    }
  }
);

router.post(
  '/assist-mode/next-action',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER', 'PATIENT'),
  async (req: AuthRequest, res: Response) => {
    try {
      const payload = req.body && typeof req.body === 'object' ? req.body : {};
      const result = await cognitiveService.getAssistModeNextAction({
        patientId: String(payload.patientId || ''),
        currentTask: payload.currentTask && typeof payload.currentTask === 'object' ? payload.currentTask : undefined,
        signals: payload.signals && typeof payload.signals === 'object' ? payload.signals : undefined,
        historySummary: payload.historySummary && typeof payload.historySummary === 'object' ? payload.historySummary : undefined,
      });

      const createAlert = Boolean(payload.createAlert);
      let alert: any = null;

      if (createAlert && canCreateAlert(req.userRole) && result.escalate) {
        alert = await alertService.create({
          patient_id: String(payload.patientId),
          task_name: String(payload.currentTask?.taskName || 'Assist Mode Session'),
          task_state: String(result.evaluation?.taskState || 'UNKNOWN'),
          severity: String(result.severity || 'MODERATE'),
          primary_issue: String(result.evaluation?.primaryIssue || 'NONE'),
          assistance_mode: String(result.assistanceMode || 'NONE'),
          engine_output: result.evaluation,
        });
      }

      res.json({
        ...result,
        alertCreated: Boolean(alert),
        alert,
      });
    } catch (error: any) {
      res.status(error.status || 500).json({ error: error.message || 'Unable to compute assist mode action' });
    }
  }
);

export default router;
