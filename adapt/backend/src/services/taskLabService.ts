import { getClient, query } from '../db/index.js';

const TASK_TYPES = ['MEDICATION', 'HYGIENE', 'MEAL', 'EXERCISE', 'SOCIAL', 'OTHER'] as const;
const RISK_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;
const COMPLEXITY_LEVELS = ['LOW', 'MEDIUM', 'HIGH'] as const;
const PLAN_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
const PLAN_SOURCES = ['TEMPLATE', 'AI', 'MANUAL'] as const;

type TaskType = (typeof TASK_TYPES)[number];
type RiskLevel = (typeof RISK_LEVELS)[number];
type ComplexityLevel = (typeof COMPLEXITY_LEVELS)[number];
type PlanStatus = (typeof PLAN_STATUSES)[number];
type PlanSource = (typeof PLAN_SOURCES)[number];

type GenericRecord = Record<string, any>;

export interface TaskPlanStepInput {
  step_order?: number;
  title: string;
  details?: string;
  is_required?: boolean;
}

export interface TaskPlanCreateInput {
  patient_id: string;
  created_by_user_id?: string;
  title: string;
  description?: string;
  scheduled_time?: string;
  task_type?: string;
  risk_level?: string;
  complexity?: string;
  status?: string;
  source?: string;
  template_key?: string;
  steps?: TaskPlanStepInput[];
}

export interface TaskPlanStep {
  id: string;
  plan_id: string;
  step_order: number;
  title: string;
  details?: string;
  is_required: boolean;
  is_completed: boolean;
  created_at: string;
  updated_at: string;
}

export interface TaskPlan {
  id: string;
  patient_id: string;
  created_by_user_id?: string;
  title: string;
  description?: string;
  scheduled_time?: string;
  task_type: TaskType;
  risk_level: RiskLevel;
  complexity: ComplexityLevel;
  status: PlanStatus;
  source: PlanSource;
  template_key?: string;
  created_at: string;
  updated_at: string;
  steps: TaskPlanStep[];
}

export interface TaskTemplate {
  key: string;
  name: string;
  description: string;
  task_type: TaskType;
  risk_level: RiskLevel;
  complexity: ComplexityLevel;
  default_time: string;
  steps: Array<{ title: string; details: string }>;
}

const TASK_TEMPLATES: TaskTemplate[] = [
  {
    key: 'medication_round',
    name: 'Medication Round',
    description: 'Timed medication adherence flow with hydration and confirmation.',
    task_type: 'MEDICATION',
    risk_level: 'HIGH',
    complexity: 'LOW',
    default_time: '08:00 AM',
    steps: [
      { title: 'Prepare Dose', details: 'Arrange medication and water in a calm space.' },
      { title: 'Guided Intake', details: 'Administer dose with one instruction at a time.' },
      { title: 'Confirm and Log', details: 'Record completion and note any side effects.' },
    ],
  },
  {
    key: 'morning_stability',
    name: 'Morning Stability Routine',
    description: 'Low-complexity morning checklist for consistency and confidence.',
    task_type: 'HYGIENE',
    risk_level: 'MEDIUM',
    complexity: 'LOW',
    default_time: '09:00 AM',
    steps: [
      { title: 'Orientation Prompt', details: 'Review today plan in short sentences.' },
      { title: 'Hygiene Sequence', details: 'Support washing and dressing with checkpoints.' },
      { title: 'Breakfast Setup', details: 'Complete simple meal and hydration confirmation.' },
    ],
  },
  {
    key: 'fall_risk_evening',
    name: 'Fall-Risk Evening Sweep',
    description: 'Safety-first evening routine with environmental checks.',
    task_type: 'EXERCISE',
    risk_level: 'HIGH',
    complexity: 'MEDIUM',
    default_time: '07:30 PM',
    steps: [
      { title: 'Environment Check', details: 'Remove obstacles and verify lighting levels.' },
      { title: 'Mobility Warm-up', details: 'Perform supervised balance and movement check.' },
      { title: 'Escalation Rule', details: 'Alert caregiver if instability is observed.' },
    ],
  },
  {
    key: 'social_engagement',
    name: 'Social Engagement Block',
    description: 'Structured social routine to reduce isolation and anxiety.',
    task_type: 'SOCIAL',
    risk_level: 'LOW',
    complexity: 'MEDIUM',
    default_time: '05:30 PM',
    steps: [
      { title: 'Warm Introduction', details: 'Start with familiar names and context cues.' },
      { title: 'Guided Interaction', details: 'Use open prompts and patience-focused pacing.' },
      { title: 'Reflection and Close', details: 'Summarize positive moments and next contact.' },
    ],
  },
];

const normalizeEnum = <T extends readonly string[]>(
  value: unknown,
  allowed: T,
  fallback: T[number]
): T[number] => {
  const normalized = String(value || fallback).trim().toUpperCase();
  return allowed.includes(normalized as T[number]) ? (normalized as T[number]) : fallback;
};

const normalizeSteps = (steps?: TaskPlanStepInput[]): TaskPlanStepInput[] => {
  const safeSteps = Array.isArray(steps) ? steps : [];
  const filtered = safeSteps
    .filter((step) => step && String(step.title || '').trim().length > 0)
    .map((step, index) => ({
      step_order: Number.isFinite(step.step_order) ? Number(step.step_order) : index + 1,
      title: String(step.title || '').trim(),
      details: step.details ? String(step.details).trim() : undefined,
      is_required: step.is_required !== false,
    }))
    .sort((a, b) => Number(a.step_order) - Number(b.step_order));

  if (filtered.length > 0) {
    return filtered.map((step, index) => ({ ...step, step_order: index + 1 }));
  }

  return [
    {
      step_order: 1,
      title: 'Prepare',
      details: 'Set up the environment and review instruction.',
      is_required: true,
    },
    {
      step_order: 2,
      title: 'Execute',
      details: 'Perform the core action with adaptive guidance.',
      is_required: true,
    },
    {
      step_order: 3,
      title: 'Confirm',
      details: 'Confirm completion and capture follow-up notes.',
      is_required: true,
    },
  ];
};

const mapPlanRows = (planRows: GenericRecord[], stepRows: GenericRecord[]): TaskPlan[] => {
  const stepsByPlan = new Map<string, TaskPlanStep[]>();

  for (const row of stepRows) {
    const planId = String(row.plan_id || '');
    if (!planId) {
      continue;
    }

    const existing = stepsByPlan.get(planId) || [];
    existing.push({
      id: String(row.id),
      plan_id: planId,
      step_order: Number(row.step_order || 0),
      title: String(row.title || ''),
      details: row.details || undefined,
      is_required: Boolean(row.is_required),
      is_completed: Boolean(row.is_completed),
      created_at: String(row.created_at),
      updated_at: String(row.updated_at),
    });
    stepsByPlan.set(planId, existing);
  }

  return planRows.map((row) => {
    const planId = String(row.id);
    return {
      id: planId,
      patient_id: String(row.patient_id),
      created_by_user_id: row.created_by_user_id || undefined,
      title: String(row.title),
      description: row.description || undefined,
      scheduled_time: row.scheduled_time || undefined,
      task_type: normalizeEnum(row.task_type, TASK_TYPES, 'OTHER'),
      risk_level: normalizeEnum(row.risk_level, RISK_LEVELS, 'MEDIUM'),
      complexity: normalizeEnum(row.complexity, COMPLEXITY_LEVELS, 'MEDIUM'),
      status: normalizeEnum(row.status, PLAN_STATUSES, 'DRAFT'),
      source: normalizeEnum(row.source, PLAN_SOURCES, 'MANUAL'),
      template_key: row.template_key || undefined,
      created_at: String(row.created_at),
      updated_at: String(row.updated_at),
      steps: (stepsByPlan.get(planId) || []).sort((a, b) => a.step_order - b.step_order),
    } satisfies TaskPlan;
  });
};

const getByIdInternal = async (planId: string): Promise<TaskPlan> => {
  const planResult = await query('SELECT * FROM task_plans WHERE id = $1 LIMIT 1', [planId]);
  if (planResult.rows.length === 0) {
    throw { status: 404, message: 'Task plan not found' };
  }

  const stepResult = await query(
    `SELECT * FROM task_plan_steps
     WHERE plan_id = $1
     ORDER BY step_order ASC`,
    [planId]
  );

  return mapPlanRows(planResult.rows, stepResult.rows)[0];
};

const getByPatientInternal = async (
  patientId: string,
  limit: number,
  offset: number,
  status?: string
): Promise<TaskPlan[]> => {
  const normalizedLimit = Math.max(1, Math.min(limit, 200));
  const normalizedOffset = Math.max(0, offset);

  const useStatus = Boolean(status && status.trim().length > 0);
  const normalizedStatus = useStatus ? normalizeEnum(status, PLAN_STATUSES, 'DRAFT') : null;

  const plansResult = useStatus
    ? await query(
        `SELECT * FROM task_plans
         WHERE patient_id = $1 AND status = $2
         ORDER BY created_at DESC
         LIMIT $3 OFFSET $4`,
        [patientId, normalizedStatus, normalizedLimit, normalizedOffset]
      )
    : await query(
        `SELECT * FROM task_plans
         WHERE patient_id = $1
         ORDER BY created_at DESC
         LIMIT $2 OFFSET $3`,
        [patientId, normalizedLimit, normalizedOffset]
      );

  if (plansResult.rows.length === 0) {
    return [];
  }

  const planIds = plansResult.rows.map((row) => String(row.id));
  const stepsResult = await query(
    `SELECT * FROM task_plan_steps
     WHERE plan_id = ANY($1::uuid[])
     ORDER BY step_order ASC`,
    [planIds]
  );

  return mapPlanRows(plansResult.rows, stepsResult.rows);
};

export const taskLabService = {
  getTemplates(): TaskTemplate[] {
    return TASK_TEMPLATES.map((template) => ({
      ...template,
      steps: template.steps.map((step) => ({ ...step })),
    }));
  },

  buildTemplateDraft(templateKey: string, patientRisk?: string): TaskPlanCreateInput {
    const template = TASK_TEMPLATES.find((item) => item.key === String(templateKey || '').trim().toLowerCase());
    if (!template) {
      throw { status: 404, message: 'Task template not found' };
    }

    return {
      patient_id: '',
      title: template.name,
      description: template.description,
      scheduled_time: template.default_time,
      task_type: template.task_type,
      risk_level: normalizeEnum(patientRisk, RISK_LEVELS, template.risk_level),
      complexity: template.complexity,
      status: 'DRAFT',
      source: 'TEMPLATE',
      template_key: template.key,
      steps: template.steps.map((step, index) => ({
        step_order: index + 1,
        title: step.title,
        details: step.details,
        is_required: true,
      })),
    };
  },

  async create(payload: TaskPlanCreateInput): Promise<TaskPlan> {
    const normalizedTaskType = normalizeEnum(payload.task_type, TASK_TYPES, 'OTHER');
    const normalizedRiskLevel = normalizeEnum(payload.risk_level, RISK_LEVELS, 'MEDIUM');
    const normalizedComplexity = normalizeEnum(payload.complexity, COMPLEXITY_LEVELS, 'MEDIUM');
    const normalizedStatus = normalizeEnum(payload.status, PLAN_STATUSES, 'DRAFT');
    const normalizedSource = normalizeEnum(payload.source, PLAN_SOURCES, 'MANUAL');

    const title = String(payload.title || '').trim();
    if (!title) {
      throw { status: 400, message: 'title is required' };
    }

    const patientId = String(payload.patient_id || '').trim();
    if (!patientId) {
      throw { status: 400, message: 'patient_id is required' };
    }

    const steps = normalizeSteps(payload.steps);
    const client = await getClient();

    try {
      await client.query('BEGIN');

      const planResult = await client.query(
        `INSERT INTO task_plans (
          patient_id,
          created_by_user_id,
          title,
          description,
          scheduled_time,
          task_type,
          risk_level,
          complexity,
          status,
          source,
          template_key
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
        RETURNING id`,
        [
          patientId,
          payload.created_by_user_id || null,
          title,
          payload.description || null,
          payload.scheduled_time || null,
          normalizedTaskType,
          normalizedRiskLevel,
          normalizedComplexity,
          normalizedStatus,
          normalizedSource,
          payload.template_key || null,
        ]
      );

      const planId = String(planResult.rows[0].id);

      for (const step of steps) {
        await client.query(
          `INSERT INTO task_plan_steps (plan_id, step_order, title, details, is_required)
           VALUES ($1, $2, $3, $4, $5)`,
          [
            planId,
            Number(step.step_order || 1),
            step.title,
            step.details || null,
            step.is_required !== false,
          ]
        );
      }

      await client.query('COMMIT');
      return await getByIdInternal(planId);
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  },

  async createFromTemplate(payload: {
    patient_id: string;
    created_by_user_id?: string;
    template_key: string;
    scheduled_time?: string;
    risk_level?: string;
  }): Promise<TaskPlan> {
    const draft = this.buildTemplateDraft(payload.template_key, payload.risk_level);
    return this.create({
      ...draft,
      patient_id: payload.patient_id,
      created_by_user_id: payload.created_by_user_id,
      scheduled_time: payload.scheduled_time || draft.scheduled_time,
    });
  },

  async getById(planId: string): Promise<TaskPlan> {
    return getByIdInternal(planId);
  },

  async getByPatientId(patientId: string, limit: number, offset: number, status?: string): Promise<TaskPlan[]> {
    return getByPatientInternal(patientId, limit, offset, status);
  },

  async updateStatus(planId: string, status: string): Promise<TaskPlan> {
    const normalizedStatus = normalizeEnum(status, PLAN_STATUSES, 'DRAFT');

    const result = await query(
      `UPDATE task_plans
       SET status = $1,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $2
       RETURNING id`,
      [normalizedStatus, planId]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Task plan not found' };
    }

    return getByIdInternal(planId);
  },

  async updateStepCompletion(planId: string, stepId: string, isCompleted: boolean): Promise<TaskPlan> {
    const result = await query(
      `UPDATE task_plan_steps
       SET is_completed = $1,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $2 AND plan_id = $3
       RETURNING id`,
      [isCompleted, stepId, planId]
    );

    if (result.rows.length === 0) {
      throw { status: 404, message: 'Task step not found for this plan' };
    }

    return getByIdInternal(planId);
  },

  async getPatientPlanCount(patientId: string): Promise<number> {
    const result = await query(
      `SELECT COUNT(*) AS count
       FROM task_plans
       WHERE patient_id = $1`,
      [patientId]
    );

    return parseInt(String(result.rows[0].count || '0'), 10);
  },
};
