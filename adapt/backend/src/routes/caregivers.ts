import { Router, Response } from 'express';
import { caregiverService } from '../services/caregiverService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

const router = Router();

// GET /api/caregivers
router.get('/', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
    const offset = parseInt(req.query.offset as string) || 0;

    const caregivers = await caregiverService.getAll(limit, offset);
    const count = await caregiverService.getCount();

    res.json({ data: caregivers, total: count, limit, offset });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/caregivers/:id
router.get('/:id', authMiddleware, requireRoles('ADMIN', 'CAREGIVER'), async (req: AuthRequest, res: Response) => {
  try {
    const caregiver = await caregiverService.getById(req.params.id);
    res.json(caregiver);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/caregivers
router.post('/', authMiddleware, requireRoles('ADMIN'), async (req: any, res: Response) => {
  try {
    const { first_name, last_name, email, phone } = req.body;

    if (!first_name || !last_name) {
      return res.status(400).json({ error: 'First name and last name are required' });
    }

    const caregiver = await caregiverService.create({
      first_name,
      last_name,
      email,
      phone,
    });

    res.status(201).json(caregiver);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// PUT /api/caregivers/:id
router.put('/:id', authMiddleware, requireRoles('ADMIN'), async (req: any, res: Response) => {
  try {
    const caregiver = await caregiverService.update(req.params.id, req.body);
    res.json(caregiver);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// DELETE /api/caregivers/:id
router.delete('/:id', authMiddleware, requireRoles('ADMIN'), async (req: AuthRequest, res: Response) => {
  try {
    await caregiverService.delete(req.params.id);
    res.json({ message: 'Caregiver deleted successfully' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
