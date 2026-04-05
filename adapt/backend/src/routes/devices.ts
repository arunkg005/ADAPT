import { Router, Response } from 'express';
import { deviceService } from '../services/deviceService.js';
import { authMiddleware, AuthRequest } from '../middleware/auth.js';

const router = Router();

// GET /api/devices
router.get('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
    const offset = parseInt(req.query.offset as string) || 0;

    const devices = await deviceService.getAll(limit, offset);
    const count = await deviceService.getCount();

    res.json({ data: devices, total: count, limit, offset });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/devices/:id
router.get('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const device = await deviceService.getById(req.params.id);
    res.json(device);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/devices
router.post('/', authMiddleware, async (req: any, res: Response) => {
  try {
    const { patient_id, device_name, device_type, os, capabilities } = req.body;

    if (!patient_id || !device_name || !device_type) {
      return res
        .status(400)
        .json({ error: 'patient_id, device_name, and device_type are required' });
    }

    const device = await deviceService.create({
      patient_id,
      device_name,
      device_type,
      os,
      capabilities,
    });

    res.status(201).json(device);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// PUT /api/devices/:id
router.put('/:id', authMiddleware, async (req: any, res: Response) => {
  try {
    const device = await deviceService.update(req.params.id, req.body);
    res.json(device);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// PATCH /api/devices/:id/status
router.patch('/:id/status', authMiddleware, async (req: any, res: Response) => {
  try {
    const { is_online } = req.body;

    if (typeof is_online !== 'boolean') {
      return res.status(400).json({ error: 'is_online boolean value is required' });
    }

    const device = await deviceService.updateStatus(req.params.id, is_online);
    res.json(device);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// DELETE /api/devices/:id
router.delete('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    await deviceService.delete(req.params.id);
    res.json({ message: 'Device deleted successfully' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
