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
/**
 * Task context: what task is being performed?
 */
export interface TaskContext {
    taskId: string;
    taskName: string;
    taskType: "MEDICATION" | "HYGIENE" | "MEAL" | "EXERCISE" | "SOCIAL" | "OTHER";
    scheduledStartTime: number;
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
    voicePromptAcknowledged: boolean;
    devicePickedUp: boolean;
    motionDetectedAfterPrompt: boolean;
    noResponseDurationMs: number;
    taskWindowElapsedMs: number;
}
/**
 * Interaction signals: direct app/voice interactions (optional, not required).
 */
export interface InteractionSignals {
    singleTapConfirm: boolean;
    voiceConfirmDone: boolean;
    voiceHelpRequest: boolean;
    instructionReplay: number;
    promptDismissed: number;
    wrongTapCount: number;
    choiceSwitches: number;
    helpRequests: number;
}
/**
 * Progress signals: how far has the task moved?
 */
export interface ProgressSignals {
    currentStepIndex: number;
    stepCompleted: boolean;
    stepRevisits: number;
    screenBacktracks: number;
    taskCompleted: boolean;
    taskAbandoned: boolean;
    idleTimeMs: number;
    responseTimeMs: number;
}
/**
 * History signals: is this a repeating pattern?
 */
export interface HistorySignals {
    recentFailureCount: number;
    recentMissedTaskCount: number;
    recentHelpFrequency: number;
    baselineResponseTimeMs: number;
    commonDifficultyType: "MEMORY" | "ATTENTION" | "SPEED" | "DECISION" | "COMPREHENSION" | "NONE";
    lastAdaptationUsed: string;
    lastAdaptationSuccess: boolean;
    consecutivePromptIgnorals: number;
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
export type TaskState = "NOT_STARTED" | "ENGAGED" | "PROGRESSING" | "STALLED" | "CONFUSED" | "ABANDONED" | "COMPLETED";
/**
 * Assistance mode: what should the system do?
 */
export type AssistanceMode = "NONE" | "SOFT_REMINDER" | "VOICE_CUE" | "GUIDED_STEP" | "ESCALATE_CARETAKER";
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
    adaptationActions: string[];
    escalationRequired: boolean;
    escalationReason?: string;
    ruleTrace: string[];
}
export interface EngineEvaluationResult {
    success: boolean;
    output?: EngineOutput;
    error?: string;
}
//# sourceMappingURL=types.d.ts.map