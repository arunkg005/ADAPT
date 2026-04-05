import { Router, Response } from 'express';
import { patientService } from '../services/patientService.js';
import { caregiverService } from '../services/caregiverService.js';
import { authMiddleware, AuthRequest } from '../middleware/auth.js';

const router = Router();

// GET /api/patients
router.get('/', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const limit = Math.min(parseInt(req.query.limit as string) || 50, 200);
    const offset = parseInt(req.query.offset as string) || 0;

    const patients = await patientService.getAll(limit, offset);
    const count = await patientService.getCount();

    res.json({ data: patients, total: count, limit, offset });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/patients/:id
router.get('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const patient = await patientService.getById(req.params.id);
    res.json(patient);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/patients
router.post('/', authMiddleware, async (req: any, res: Response) => {
  try {
    const { first_name, last_name, date_of_birth, cognitive_condition, risk_level, baseline_response_time_ms } =
      req.body;

    if (!first_name || !last_name) {
      return res.status(400).json({ error: 'First name and last name are required' });
    }

    const patient = await patientService.create({
      first_name,
      last_name,
      date_of_birth,
      cognitive_condition,
      risk_level,
      baseline_response_time_ms,
    });

    res.status(201).json(patient);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// PUT /api/patients/:id
router.put('/:id', authMiddleware, async (req: any, res: Response) => {
  try {
    const patient = await patientService.update(req.params.id, req.body);
    res.json(patient);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// DELETE /api/patients/:id
router.delete('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    await patientService.delete(req.params.id);
    res.json({ message: 'Patient deleted successfully' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/patients/:id/caregivers
router.get('/:id/caregivers', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const caregivers = await caregiverService.getByPatientId(req.params.id);
    res.json(caregivers);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/patients/:patientId/caregivers/:caregiverId
router.post('/:patientId/caregivers/:caregiverId', authMiddleware, async (req: any, res: Response) => {
  try {
    const { relationship } = req.body;
    await caregiverService.assignToPatient(req.params.patientId, req.params.caregiverId, relationship);
    res.json({ message: 'Caregiver assigned to patient' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// DELETE /api/patients/:patientId/caregivers/:caregiverId
router.delete('/:patientId/caregivers/:caregiverId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    await caregiverService.removeFromPatient(req.params.patientId, req.params.caregiverId);
    res.json({ message: 'Caregiver removed from patient' });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
