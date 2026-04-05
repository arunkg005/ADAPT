import { Router, Response } from 'express';
import { telemetryService } from '../services/telemetryService.js';
import { alertService } from '../services/alertService.js';
import { engineService } from '../services/engineService.js';
import { authMiddleware, AuthRequest } from '../middleware/auth.js';

const router = Router();

// POST /api/telemetry
router.post('/', authMiddleware, async (req: any, res: Response) => {
  try {
    const { patient_id, device_id, signal_type, signal_value, timestamp_ms } = req.body;

    if (!patient_id || !signal_type || !signal_value) {
      return res.status(400).json({ error: 'patient_id, signal_type, and signal_value are required' });
    }

    // Store telemetry
    const telemetry = await telemetryService.create({
      patient_id,
      device_id,
      signal_type,
      signal_value,
      timestamp_ms: timestamp_ms || Date.now(),
    });

    // TODO: In production, batch telemetry and call engine periodically
    // For now, just acknowledge receipt
    res.status(201).json({ message: 'Telemetry received', telemetry });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/telemetry/:patientId
router.get('/:patientId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { signalType, limitMs } = req.query;

    const telemetry = await telemetryService.getByPatientId(
      req.params.patientId,
      signalType as string | undefined,
      limitMs ? parseInt(limitMs as string) : 3600000
    );

    res.json(telemetry);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// GET /api/telemetry/device/:deviceId
router.get('/device/:deviceId', authMiddleware, async (req: AuthRequest, res: Response) => {
  try {
    const { limitMs } = req.query;

    const telemetry = await telemetryService.getByDeviceId(
      req.params.deviceId,
      limitMs ? parseInt(limitMs as string) : 3600000
    );

    res.json(telemetry);
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

// POST /api/telemetry/evaluate (manually trigger engine evaluation)
router.post('/evaluate/:patientId', authMiddleware, async (req: any, res: Response) => {
  try {
    const { taskContext, limitMs } = req.body;

    if (!taskContext) {
      return res.status(400).json({ error: 'taskContext is required' });
    }

    // Gather recent telemetry for patient
    const recentTelemetry = await telemetryService.getByPatientId(
      req.params.patientId,
      undefined,
      limitMs || 600000
    );

    // TODO: Build EngineInput from taskContext and recentTelemetry
    // Call engineService.evaluate()
    // Create alert if severity > NONE

    res.json({
      message: 'Manual evaluation triggered',
      patientId: req.params.patientId,
      telemetryCount: recentTelemetry.length,
    });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
