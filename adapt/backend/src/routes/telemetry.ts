import { Router, Response } from 'express';
import { telemetryService } from '../services/telemetryService.js';
import { alertService } from '../services/alertService.js';
import { engineService } from '../services/engineService.js';
import { authMiddleware, AuthRequest, requireRoles } from '../middleware/auth.js';

type GenericRecord = Record<string, any>;

const TASK_TYPES = ['MEDICATION', 'HYGIENE', 'MEAL', 'EXERCISE', 'SOCIAL', 'OTHER'] as const;
const RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;
const COMPLEXITY_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;

const isObject = (value: unknown): value is GenericRecord => {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
};

const clampNumber = (value: unknown, fallback: number, min: number, max: number): number => {
  const parsed = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.max(min, Math.min(max, parsed));
};

const normalizeEnum = <T extends readonly string[]>(
  value: unknown,
  allowed: T,
  fallback: T[number]
): T[number] => {
  const normalized = String(value || fallback).trim().toUpperCase();
  return allowed.includes(normalized as T[number]) ? (normalized as T[number]) : fallback;
};

const createSignalBuckets = () => ({
  passiveSignals: {
    reminderDelivered: false,
    voicePromptDelivered: false,
    voicePromptAcknowledged: false,
    devicePickedUp: false,
    motionDetectedAfterPrompt: false,
    noResponseDurationMs: 0,
    taskWindowElapsedMs: 0,
  },
  interactionSignals: {
    singleTapConfirm: false,
    voiceConfirmDone: false,
    voiceHelpRequest: false,
    instructionReplay: 0,
    promptDismissed: 0,
    wrongTapCount: 0,
    choiceSwitches: 0,
    helpRequests: 0,
  },
  progressSignals: {
    currentStepIndex: 0,
    stepCompleted: false,
    stepRevisits: 0,
    screenBacktracks: 0,
    taskCompleted: false,
    taskAbandoned: false,
    idleTimeMs: 0,
    responseTimeMs: 0,
  },
  historySignals: {
    recentFailureCount: 0,
    recentMissedTaskCount: 0,
    recentHelpFrequency: 0,
    baselineResponseTimeMs: 15000,
    commonDifficultyType: 'NONE',
    lastAdaptationUsed: 'NONE',
    lastAdaptationSuccess: false,
    consecutivePromptIgnorals: 0,
  },
});

const mergeKnownFields = (target: GenericRecord, source: GenericRecord) => {
  for (const [key, value] of Object.entries(source)) {
    if (key in target) {
      target[key] = value;
    }
  }
};

const applySignal = (
  signalType: string,
  signalValue: unknown,
  buckets: ReturnType<typeof createSignalBuckets>
) => {
  const trimmedType = signalType.trim();
  const [bucketName, bucketKey] = trimmedType.split('.', 2);

  const bucketMap: Record<string, GenericRecord> = {
    passive: buckets.passiveSignals,
    passiveSignals: buckets.passiveSignals,
    interaction: buckets.interactionSignals,
    interactionSignals: buckets.interactionSignals,
    progress: buckets.progressSignals,
    progressSignals: buckets.progressSignals,
    history: buckets.historySignals,
    historySignals: buckets.historySignals,
  };

  const bucket = bucketMap[bucketName] || null;
  if (!bucket) {
    return;
  }

  if (bucketKey) {
    if (bucketKey in bucket) {
      bucket[bucketKey] = signalValue;
    }
    return;
  }

  if (isObject(signalValue)) {
    mergeKnownFields(bucket, signalValue);
  }
};

const buildEngineInput = (patientId: string, taskContext: GenericRecord, telemetryRows: GenericRecord[]) => {
  const boundedExpectedDuration = clampNumber(taskContext.expectedDurationMs, 300000, 30000, 3600000);
  const buckets = createSignalBuckets();

  for (const telemetry of telemetryRows) {
    const signalType = String(telemetry.signal_type || '');
    if (!signalType) {
      continue;
    }

    applySignal(signalType, telemetry.signal_value, buckets);
  }

  return {
    patientId,
    taskContext: {
      taskId: String(taskContext.taskId || `manual-${Date.now()}`),
      taskName: String(taskContext.taskName || 'Assisted Task'),
      taskType: normalizeEnum(taskContext.taskType, TASK_TYPES, 'OTHER'),
      scheduledStartTime: clampNumber(taskContext.scheduledStartTime, Date.now(), 0, Number.MAX_SAFE_INTEGER),
      expectedDurationMs: boundedExpectedDuration,
      stepCount: Math.floor(clampNumber(taskContext.stepCount, 1, 1, 1000)),
      riskLevel: normalizeEnum(taskContext.riskLevel, RISK_LEVELS, 'MEDIUM'),
      complexity: normalizeEnum(taskContext.complexity, COMPLEXITY_LEVELS, 'MEDIUM'),
    },
    ...buckets,
  };
};

const router = Router();

// POST /api/telemetry
router.post(
  '/',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER', 'PATIENT'),
  async (req: any, res: Response) => {
  try {
    const { patient_id, device_id, signal_type, signal_value, timestamp_ms } = req.body;

    if (!patient_id || !signal_type || signal_value === undefined || signal_value === null) {
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
router.get(
  '/:patientId',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
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
router.get(
  '/device/:deviceId',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: AuthRequest, res: Response) => {
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
router.post(
  '/evaluate/:patientId',
  authMiddleware,
  requireRoles('ADMIN', 'CAREGIVER'),
  async (req: any, res: Response) => {
  try {
    const { taskContext, limitMs } = req.body;

    if (!taskContext) {
      return res.status(400).json({ error: 'taskContext is required' });
    }

    // Gather recent telemetry for patient
    const recentTelemetry = await telemetryService.getByPatientId(
      req.params.patientId,
      undefined,
      clampNumber(limitMs, 600000, 60000, 86400000)
    );

    const engineInput = buildEngineInput(
      req.params.patientId,
      isObject(taskContext) ? taskContext : {},
      recentTelemetry as GenericRecord[]
    );

    const evaluation = await engineService.evaluate(engineInput);

    let alert: any = null;
    if (evaluation.severity !== 'NONE') {
      alert = await alertService.create({
        patient_id: req.params.patientId,
        task_name: engineInput.taskContext.taskName,
        task_state: evaluation.taskState,
        severity: evaluation.severity,
        primary_issue: evaluation.primaryIssue,
        assistance_mode: evaluation.assistanceMode,
        engine_output: evaluation,
      });
    }

    res.json({
      message: 'Manual evaluation completed',
      patientId: req.params.patientId,
      telemetryCount: recentTelemetry.length,
      evaluation,
      alertCreated: Boolean(alert),
      alert,
    });
  } catch (error: any) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

export default router;
