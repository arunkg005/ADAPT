/**
 * ADAPT Rule Engine - Data Model
 * 
 * Defines all input and output types for the behavioral rule engine.
 * Based on the 5-layer input architecture:
 * 1. Task context
 * 2. Passive signals
 * 3. Interaction signals
 * 4. Progress signals
 * 5. History signals
 */

// ============================================================================
// INPUT TYPES
// ============================================================================

/**
 * Task context: what task is being performed?
 */
export interface TaskContext {
  taskId: string;
  taskName: string;
  taskType: "MEDICATION" | "HYGIENE" | "MEAL" | "EXERCISE" | "SOCIAL" | "OTHER";
  scheduledStartTime: number; // unix timestamp
  expectedDurationMs: number;
  stepCount: number;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  complexity: "LOW" | "MEDIUM" | "HIGH";
}

/**
 * Passive signals: did the patient respond to the environment/prompt?
 * These are hands-free observations, not screen taps.
 */
export interface PassiveSignals {
  reminderDelivered: boolean;
  voicePromptDelivered: boolean;
  voicePromptAcknowledged: boolean; // did patient acknowledge the voice prompt?
  devicePickedUp: boolean;
  motionDetectedAfterPrompt: boolean;
  noResponseDurationMs: number; // how long of silence/inactivity?
  taskWindowElapsedMs: number; // time since task was scheduled
}

/**
 * Interaction signals: direct app/voice interactions (optional, not required).
 */
export interface InteractionSignals {
  singleTapConfirm: boolean;
  voiceConfirmDone: boolean;
  voiceHelpRequest: boolean;
  instructionReplay: number; // how many times did patient replay?
  promptDismissed: number;
  wrongTapCount: number; // wrong button presses
  choiceSwitches: number; // changing mind between options
  helpRequests: number;
}

/**
 * Progress signals: how far has the task moved?
 */
export interface ProgressSignals {
  currentStepIndex: number; // 0-based
  stepCompleted: boolean; // did current step complete?
  stepRevisits: number; // how many times did they revisit the same step?
  screenBacktracks: number; // how many times did they go back?
  taskCompleted: boolean;
  taskAbandoned: boolean;
  idleTimeMs: number; // time spent doing nothing during task
  responseTimeMs: number; // time to respond to latest prompt
}

/**
 * History signals: is this a repeating pattern?
 */
export interface HistorySignals {
  recentFailureCount: number; // task failures in last N days
  recentMissedTaskCount: number;
  recentHelpFrequency: number; // 0–100 scale
  baselineResponseTimeMs: number; // patient's typical response time
  commonDifficultyType: "MEMORY" | "ATTENTION" | "SPEED" | "DECISION" | "COMPREHENSION" | "NONE";
  lastAdaptationUsed: string; // e.g., "SIMPLIFIED_STEPS", "VOICE_ONLY"
  lastAdaptationSuccess: boolean;
  consecutivePromptIgnorals: number; // how many times did they ignore prompts in a row?
}

/**
 * Complete engine input: all 5 blocks together.
 */
export interface EngineInput {
  patientId: string;
  taskContext: TaskContext;
  passiveSignals: PassiveSignals;
  interactionSignals: InteractionSignals;
  progressSignals: ProgressSignals;
  historySignals: HistorySignals;
}

// ============================================================================
// OUTPUT TYPES
// ============================================================================

/**
 * Cognitive difficulty scores (0–100 scale).
 * Higher = more likely.
 */
export interface CognitiveScores {
  workingMemory: number;
  attention: number;
  processingSpeed: number;
  decisionMaking: number;
  comprehension: number;
}

/**
 * Task state classification.
 */
export type TaskState =
  | "NOT_STARTED"
  | "ENGAGED"
  | "PROGRESSING"
  | "STALLED"
  | "CONFUSED"
  | "ABANDONED"
  | "COMPLETED";

/**
 * Assistance mode: what should the system do?
 */
export type AssistanceMode =
  | "NONE"
  | "SOFT_REMINDER"
  | "VOICE_CUE"
  | "GUIDED_STEP"
  | "ESCALATE_CARETAKER";

/**
 * Severity level.
 */
export type Severity = "NONE" | "MILD" | "MODERATE" | "SEVERE" | "CRITICAL";

/**
 * Confidence in the engine's assessment (0–100).
 */
export type Confidence = number;

/**
 * Complete engine output.
 */
export interface EngineOutput {
  taskState: TaskState;
  cognitiveScores: CognitiveScores;
  primaryIssue: keyof CognitiveScores | "NONE";
  secondaryIssue: keyof CognitiveScores | "NONE";
  severity: Severity;
  confidence: Confidence;
  assistanceMode: AssistanceMode;
  adaptationActions: string[]; // e.g., ["SIMPLIFY_STEP", "ENABLE_VOICE", "EXTEND_TIMEOUT"]
  escalationRequired: boolean;
  escalationReason?: string; // if escalation needed
  ruleTrace: string[]; // human-readable log of rules that fired
}

// ============================================================================
// UTILITY TYPES
// ============================================================================

export interface EngineEvaluationResult {
  success: boolean;
  output?: EngineOutput;
  error?: string;
}
