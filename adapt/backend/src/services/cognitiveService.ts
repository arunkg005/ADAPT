import { engineService } from './engineService.js';

type GenericRecord = Record<string, any>;

const TASK_TYPES = ['MEDICATION', 'HYGIENE', 'MEAL', 'EXERCISE', 'SOCIAL', 'OTHER'] as const;
const RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;
const COMPLEXITY_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;

const normalizeEnum = <T extends readonly string[]>(
  value: unknown,
  allowed: T,
  fallback: T[number]
): T[number] => {
  const normalized = String(value || fallback).trim().toUpperCase();
  return allowed.includes(normalized as T[number]) ? (normalized as T[number]) : fallback;
};

const clampNumber = (value: unknown, fallback: number, min: number, max: number): number => {
  const parsed = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.max(min, Math.min(max, parsed));
};

const defaultSignals = () => ({
  passiveSignals: {
    reminderDelivered: true,
    voicePromptDelivered: true,
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

const mergeKnownFields = (target: GenericRecord, incoming: GenericRecord) => {
  for (const [key, value] of Object.entries(incoming)) {
    if (key in target) {
      target[key] = value;
    }
  }
};

interface AssistModeRequest {
  patientId: string;
  currentTask?: GenericRecord;
  signals?: {
    passiveSignals?: GenericRecord;
    interactionSignals?: GenericRecord;
    progressSignals?: GenericRecord;
    historySignals?: GenericRecord;
  };
  historySummary?: GenericRecord;
}

export const cognitiveService = {
  async evaluate(engineInput: GenericRecord): Promise<GenericRecord> {
    return engineService.evaluate(engineInput as any);
  },

  async getAssistModeNextAction(payload: AssistModeRequest): Promise<GenericRecord> {
    const patientId = String(payload.patientId || '').trim();
    if (!patientId) {
      throw { status: 400, message: 'patientId is required' };
    }

    const task = payload.currentTask || {};
    const base = defaultSignals();

    if (payload.signals?.passiveSignals) {
      mergeKnownFields(base.passiveSignals, payload.signals.passiveSignals);
    }

    if (payload.signals?.interactionSignals) {
      mergeKnownFields(base.interactionSignals, payload.signals.interactionSignals);
    }

    if (payload.signals?.progressSignals) {
      mergeKnownFields(base.progressSignals, payload.signals.progressSignals);
    }

    if (payload.signals?.historySignals) {
      mergeKnownFields(base.historySignals, payload.signals.historySignals);
    }

    if (payload.historySummary) {
      mergeKnownFields(base.historySignals, payload.historySummary);
    }

    const engineInput = {
      patientId,
      taskContext: {
        taskId: String(task.taskId || `assist-${Date.now()}`),
        taskName: String(task.taskName || 'Assist Session Task'),
        taskType: normalizeEnum(task.taskType, TASK_TYPES, 'OTHER'),
        scheduledStartTime: clampNumber(task.scheduledStartTime, Date.now(), 0, Number.MAX_SAFE_INTEGER),
        expectedDurationMs: clampNumber(task.expectedDurationMs, 300000, 30000, 3600000),
        stepCount: Math.floor(clampNumber(task.stepCount, 1, 1, 1000)),
        riskLevel: normalizeEnum(task.riskLevel, RISK_LEVELS, 'MEDIUM'),
        complexity: normalizeEnum(task.complexity, COMPLEXITY_LEVELS, 'MEDIUM'),
      },
      ...base,
    };

    const evaluation = await engineService.evaluate(engineInput);

    const actionType =
      evaluation.assistanceMode === 'ESCALATE_CARETAKER'
        ? 'ESCALATE'
        : evaluation.assistanceMode === 'GUIDED_STEP'
        ? 'GUIDED_STEP'
        : evaluation.assistanceMode === 'VOICE_CUE'
        ? 'VOICE_CUE'
        : evaluation.assistanceMode === 'SOFT_REMINDER'
        ? 'REMINDER'
        : 'NONE';

    const severity = String(evaluation.severity || 'NONE');
    const primaryIssue = String(evaluation.primaryIssue || 'NONE');

    const uiPrompt =
      actionType === 'ESCALATE'
        ? 'Caregiver escalation recommended. Keep patient calm and wait for support.'
        : actionType === 'GUIDED_STEP'
        ? `Guide one step at a time. Focus on ${primaryIssue.toLowerCase()} support.`
        : actionType === 'VOICE_CUE'
        ? 'Use short voice prompts and repeat once if needed.'
        : actionType === 'REMINDER'
        ? 'Deliver a soft reminder and observe response for 60 seconds.'
        : 'Patient appears stable. Continue routine monitoring.';

    const voicePrompt =
      actionType === 'ESCALATE'
        ? 'Support is being contacted. Stay calm and remain seated.'
        : actionType === 'GUIDED_STEP'
        ? 'Let us do one step together now.'
        : actionType === 'VOICE_CUE'
        ? 'Please follow the next instruction slowly.'
        : actionType === 'REMINDER'
        ? 'This is a gentle reminder for your current task.'
        : 'You are doing well. Continue with your routine.';

    return {
      actionType,
      uiPrompt,
      voicePrompt,
      assistanceMode: evaluation.assistanceMode,
      escalate: actionType === 'ESCALATE',
      escalationReason: evaluation.escalationReason,
      severity,
      evaluation,
    };
  },
};
