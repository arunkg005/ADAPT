import { Router, Response } from 'express';
import { aiService } from '../services/aiService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

router.post('/chat', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const { prompt, patientId, roleContext, conversationHistory } = req.body || {};

    if (!prompt || String(prompt).trim().length === 0) {
      return res.status(400).json({ error: 'prompt is required' });
    }

    const result = await aiService.chat({
      prompt: String(prompt),
      patientId: patientId ? String(patientId) : undefined,
      roleContext: roleContext ? String(roleContext) : undefined,
      conversationHistory: Array.isArray(conversationHistory) ? conversationHistory : undefined,
    });

    res.json(result);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message || 'AI chat failed' });
  }
});

router.post(
  '/task-lab/generate',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
    try {
      const { prompt, patientProfile, constraints } = req.body || {};

      if (!prompt || String(prompt).trim().length === 0) {
        return res.status(400).json({ error: 'prompt is required' });
      }

      const result = await aiService.generateTaskLabDraft({
        prompt: String(prompt),
        patientProfile: patientProfile && typeof patientProfile === 'object' ? patientProfile : undefined,
        constraints: constraints && typeof constraints === 'object' ? constraints : undefined,
      });

      res.json({
        message: 'Task Lab draft generated',
        ...result,
      });
    } catch (error: any) {
      res.status(error.status || 500).json({ error: error.message || 'Task generation failed' });
    }
  }
);

export default router;
