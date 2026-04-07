import { Router, Response } from 'express';
import { analysisService } from '../services/analysisService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

router.get('/overview', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const windowMs = req.query.windowMs ? parseInt(String(req.query.windowMs), 10) : 86_400_000;
    const overview = await analysisService.getOverview(windowMs);
    res.json(overview);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'Unable to load analysis overview' });
  }
});

router.get(
  '/patient/:patientId/summary',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
    try {
      const windowMs = req.query.windowMs ? parseInt(String(req.query.windowMs), 10) : 86_400_000;
      const summary = await analysisService.getPatientSummary(req.params.patientId, windowMs);
      res.json(summary);
    } catch (error: any) {
      res.status(error.status || 500).json({ error: error.message || 'Unable to load patient analysis summary' });
    }
  }
);

export default router;
