import { taskLabService, TaskPlanCreateInput, TaskPlanStepInput } from './taskLabService.js';
import { googleAiService } from './googleAiService.js';

type GenericRecord = Record<string, any>;

type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
type ComplexityLevel = 'LOW' | 'MEDIUM' | 'HIGH';
type TaskType = 'MEDICATION' | 'HYGIENE' | 'MEAL' | 'EXERCISE' | 'SOCIAL' | 'OTHER';

interface ChatResponse {
  reply: string;
  actionItems: string[];
  safetyFlags: string[];
  confidence: number;
}

interface ChatRequest {
  prompt: string;
  patientId?: string;
  roleContext?: string;
  conversationHistory?: Array<{ role: string; content: string }>;
}

interface TaskGenerationRequest {
  prompt: string;
  patientProfile?: GenericRecord;
  constraints?: GenericRecord;
}

interface TaskGenerationResponse {
  guidance: string;
  draft: Omit<TaskPlanCreateInput, 'patient_id' | 'created_by_user_id'>;
}

const TASK_TYPES: TaskType[] = ['MEDICATION', 'HYGIENE', 'MEAL', 'EXERCISE', 'SOCIAL', 'OTHER'];

const normalizeText = (value: unknown): string => String(value || '').trim();

const normalizeRisk = (value: unknown, fallback: RiskLevel = 'MEDIUM'): RiskLevel => {
  const normalized = String(value || fallback).trim().toUpperCase();
  if (normalized === 'LOW' || normalized === 'MEDIUM' || normalized === 'HIGH') {
    return normalized;
  }

  return fallback;
};

const normalizeComplexity = (value: unknown, fallback: ComplexityLevel = 'MEDIUM'): ComplexityLevel => {
  const normalized = String(value || fallback).trim().toUpperCase();
  if (normalized === 'LOW' || normalized === 'MEDIUM' || normalized === 'HIGH') {
    return normalized;
  }

  return fallback;
};

const normalizeTaskType = (value: unknown, fallback: TaskType = 'OTHER'): TaskType => {
  const normalized = normalizeText(value).toUpperCase();
  if (TASK_TYPES.includes(normalized as TaskType)) {
    return normalized as TaskType;
  }

  return fallback;
};

const inferTaskType = (prompt: string): TaskType => {
  const text = prompt.toLowerCase();
  if (text.includes('medication') || text.includes('dose') || text.includes('pill')) {
    return 'MEDICATION';
  }

  if (text.includes('hygiene') || text.includes('wash') || text.includes('bath')) {
    return 'HYGIENE';
  }

  if (text.includes('meal') || text.includes('nutrition') || text.includes('breakfast') || text.includes('dinner')) {
    return 'MEAL';
  }

  if (text.includes('fall') || text.includes('mobility') || text.includes('balance') || text.includes('exercise')) {
    return 'EXERCISE';
  }

  if (text.includes('social') || text.includes('call') || text.includes('family')) {
    return 'SOCIAL';
  }

  return 'OTHER';
};

const inferRisk = (prompt: string): RiskLevel => {
  const text = prompt.toLowerCase();
  if (text.includes('critical') || text.includes('high risk') || text.includes('urgent') || text.includes('fall')) {
    return 'HIGH';
  }

  if (text.includes('low risk') || text.includes('light')) {
    return 'LOW';
  }

  return 'MEDIUM';
};

const inferComplexity = (prompt: string): ComplexityLevel => {
  const text = prompt.toLowerCase();
  if (text.includes('simple') || text.includes('easy') || text.includes('one-step')) {
    return 'LOW';
  }

  if (text.includes('advanced') || text.includes('detailed') || text.includes('multi-step')) {
    return 'HIGH';
  }

  return 'MEDIUM';
};

const buildDeterministicSteps = (prompt: string): TaskPlanStepInput[] => {
  const taskType = inferTaskType(prompt);

  if (taskType === 'MEDICATION') {
    return [
      { step_order: 1, title: 'Preparation', details: 'Prepare medication and hydration in a distraction-free area.', is_required: true },
      { step_order: 2, title: 'Guided Intake', details: 'Deliver one instruction at a time and confirm completion.', is_required: true },
      { step_order: 3, title: 'Post-Check', details: 'Observe patient response and log adherence outcomes.', is_required: true },
    ];
  }

  if (taskType === 'EXERCISE') {
    return [
      { step_order: 1, title: 'Safety Sweep', details: 'Check environment hazards and ensure support aids are available.', is_required: true },
      { step_order: 2, title: 'Guided Movement', details: 'Execute low-impact movement with close supervision.', is_required: true },
      { step_order: 3, title: 'Stability Confirmation', details: 'Confirm comfort and no dizziness before ending routine.', is_required: true },
    ];
  }

  if (taskType === 'SOCIAL') {
    return [
      { step_order: 1, title: 'Warm Start', details: 'Use familiar context cues and positive orientation.', is_required: true },
      { step_order: 2, title: 'Engagement Segment', details: 'Guide conversation with clear prompts and pauses.', is_required: true },
      { step_order: 3, title: 'Closure Note', details: 'Summarize interaction and schedule next engagement.', is_required: true },
    ];
  }

  return [
    { step_order: 1, title: 'Prepare', details: 'Set environment and review instructions.', is_required: true },
    { step_order: 2, title: 'Execute', details: 'Complete the key action with adaptive prompts.', is_required: true },
    { step_order: 3, title: 'Confirm', details: 'Record completion and follow-up actions.', is_required: true },
  ];
};

const inferScheduledTime = (prompt: string): string => {
  const text = prompt.toLowerCase();
  if (text.includes('morning')) {
    return '08:30 AM';
  }

  if (text.includes('afternoon')) {
    return '02:00 PM';
  }

  if (text.includes('evening') || text.includes('night')) {
    return '07:30 PM';
  }

  return '09:00 AM';
};

const titleFromPrompt = (prompt: string): string => {
  const cleaned = prompt.trim().replace(/\s+/g, ' ');
  if (cleaned.length <= 60) {
    return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
  }

  return cleaned.slice(0, 57).trimEnd() + '...';
};

const normalizeConfidence = (value: unknown, fallback = 0.82): number => {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return fallback;
  }

  return Math.max(0, Math.min(1, parsed));
};

const toStringArray = (value: unknown): string[] => {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((entry) => normalizeText(entry))
    .filter((entry) => entry.length > 0);
};

const buildFallbackChatResponse = (prompt: string): ChatResponse => {
  const normalized = prompt.toLowerCase();
  const actionItems: string[] = [];
  const safetyFlags: string[] = [];

  if (normalized.includes('fall')) {
    actionItems.push('Increase observation cadence to every 15 minutes.');
    actionItems.push('Enable assist mode with mobility-safe prompts.');
    safetyFlags.push('FALL_RISK');
  }

  if (normalized.includes('medication')) {
    actionItems.push('Set three timed reminders with acknowledgement requirement.');
    actionItems.push('Escalate after two missed confirmations.');
    safetyFlags.push('MEDICATION_ADHERENCE');
  }

  if (actionItems.length === 0) {
    actionItems.push('Use Task Lab templates for quick structured planning.');
    actionItems.push('Track response time and missed-step trends in Analysis Board.');
  }

  return {
    reply: 'Care plan guidance generated. Use Task Lab to publish a structured routine and monitor outcomes in Analysis Board.',
    actionItems,
    safetyFlags,
    confidence: 0.82,
  };
};

const ensureActionItems = (value: unknown, prompt: string): string[] => {
  const items = toStringArray(value)
    .map((entry) => entry.replace(/\s+/g, ' ').trim())
    .filter((entry) => entry.length >= 4)
    .slice(0, 4);

  if (items.length > 0) {
    return items;
  }

  return buildFallbackChatResponse(prompt).actionItems;
};

const ensureSafetyFlags = (value: unknown, prompt: string): string[] => {
  const flags = toStringArray(value)
    .map((entry) => entry.toUpperCase().replace(/[^A-Z0-9_]/g, '_'))
    .map((entry) => entry.replace(/_+/g, '_').replace(/^_+|_+$/g, ''))
    .filter((entry) => entry.length > 0);

  const normalizedPrompt = prompt.toLowerCase();
  if (normalizedPrompt.includes('fall')) {
    flags.push('FALL_RISK');
  }

  if (normalizedPrompt.includes('medication')) {
    flags.push('MEDICATION_ADHERENCE');
  }

  return Array.from(new Set(flags));
};

const normalizeGeneratedSteps = (value: unknown, prompt: string): TaskPlanStepInput[] => {
  const stepCandidates = Array.isArray(value) ? value : [];

  const steps: TaskPlanStepInput[] = [];
  for (let index = 0; index < stepCandidates.length; index += 1) {
    const step = stepCandidates[index];
    const candidate = step && typeof step === 'object' ? (step as GenericRecord) : {};
    const title = normalizeText(candidate.title || candidate.step || candidate.name);
    if (!title) {
      continue;
    }

    steps.push({
      step_order: index + 1,
      title,
      details: normalizeText(candidate.details || candidate.description || candidate.instructions) || undefined,
      is_required: candidate.is_required !== false,
    });
  }

  if (steps.length > 0) {
    return steps;
  }

  return buildDeterministicSteps(prompt);
};

const buildDeterministicTaskDraft = (
  prompt: string,
  payload: TaskGenerationRequest
): TaskGenerationResponse => {
  const inferredRisk = normalizeRisk(payload.constraints?.riskLevel || payload.patientProfile?.risk_level || inferRisk(prompt));
  const inferredComplexity = normalizeComplexity(payload.constraints?.complexity || inferComplexity(prompt));

  return {
    guidance: 'AI draft generated with adaptive step structure and risk-aware sequencing.',
    draft: {
      title: titleFromPrompt(prompt),
      description: 'AI-generated routine draft. Review and publish in Task Lab.',
      scheduled_time: String(payload.constraints?.scheduledTime || inferScheduledTime(prompt)),
      task_type: inferTaskType(prompt),
      risk_level: inferredRisk,
      complexity: inferredComplexity,
      status: 'DRAFT',
      source: 'AI',
      steps: buildDeterministicSteps(prompt),
    },
  };
};

export const aiService = {
  async chat(payload: ChatRequest): Promise<GenericRecord> {
    const prompt = String(payload.prompt || '').trim();
    if (!prompt) {
      throw { status: 400, message: 'prompt is required' };
    }

    if (googleAiService.isConfigured()) {
      try {
        const aiResult = await googleAiService.generateChat({
          prompt,
          roleContext: payload.roleContext,
          conversationHistory: payload.conversationHistory,
        });

        const fallback = buildFallbackChatResponse(prompt);
        return {
          reply: normalizeText(aiResult.reply) || fallback.reply,
          actionItems: ensureActionItems(aiResult.actionItems, prompt),
          safetyFlags: ensureSafetyFlags(aiResult.safetyFlags, prompt),
          confidence: normalizeConfidence(aiResult.confidence, fallback.confidence),
        };
      } catch (error) {
        console.error('Gemini chat failed, using deterministic fallback:', error);
      }
    }

    return buildFallbackChatResponse(prompt);
  },

  async generateTaskLabDraft(payload: TaskGenerationRequest): Promise<TaskGenerationResponse> {
    const prompt = String(payload.prompt || '').trim();
    if (!prompt) {
      throw { status: 400, message: 'prompt is required' };
    }

    const lowercase = prompt.toLowerCase();
    if (lowercase.includes('template:')) {
      const templateKey = lowercase.split('template:')[1]?.trim().split(/\s+/)[0] || '';
      if (templateKey) {
        const templateDraft = taskLabService.buildTemplateDraft(templateKey, payload.constraints?.riskLevel);
        return {
          guidance: 'Template-based draft generated for fast publication.',
          draft: {
            title: templateDraft.title,
            description: templateDraft.description,
            scheduled_time: templateDraft.scheduled_time,
            task_type: templateDraft.task_type,
            risk_level: templateDraft.risk_level,
            complexity: templateDraft.complexity,
            status: 'DRAFT',
            source: 'TEMPLATE',
            template_key: templateDraft.template_key,
            steps: templateDraft.steps,
          },
        };
      }
    }

    if (googleAiService.isConfigured()) {
      try {
        const aiResult = await googleAiService.generateTaskDraft({
          prompt,
          patientProfile: payload.patientProfile,
          constraints: payload.constraints,
        });

        const aiDraft = aiResult?.draft && typeof aiResult.draft === 'object' ? (aiResult.draft as GenericRecord) : {};
        const inferredRisk = normalizeRisk(payload.constraints?.riskLevel || payload.patientProfile?.risk_level || inferRisk(prompt));
        const inferredComplexity = normalizeComplexity(payload.constraints?.complexity || inferComplexity(prompt));
        const templateKey = normalizeText(aiDraft.template_key);

        return {
          guidance:
            normalizeText(aiResult?.guidance) ||
            'AI draft generated with adaptive step structure and risk-aware sequencing.',
          draft: {
            title: normalizeText(aiDraft.title) || titleFromPrompt(prompt),
            description:
              normalizeText(aiDraft.description) || 'AI-generated routine draft. Review and publish in Task Lab.',
            scheduled_time:
              normalizeText(aiDraft.scheduled_time) || String(payload.constraints?.scheduledTime || inferScheduledTime(prompt)),
            task_type: normalizeTaskType(aiDraft.task_type, inferTaskType(prompt)),
            risk_level: normalizeRisk(aiDraft.risk_level, inferredRisk),
            complexity: normalizeComplexity(aiDraft.complexity, inferredComplexity),
            status: 'DRAFT',
            source: 'AI',
            ...(templateKey ? { template_key: templateKey } : {}),
            steps: normalizeGeneratedSteps(aiDraft.steps, prompt),
          },
        };
      } catch (error) {
        console.error('Gemini draft generation failed, using deterministic fallback:', error);
      }
    }

    return buildDeterministicTaskDraft(prompt, payload);
  },
};
