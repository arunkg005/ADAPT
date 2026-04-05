/**
 * ADAPT Rule Engine - Core Logic
 *
 * Implements the 5-step evaluation sequence:
 * 1. Determine task state
 * 2. Score cognitive difficulty
 * 3. Apply severity and confidence
 * 4. Select assistance mode
 * 5. Build output with rule trace
 */
import { EngineInput, EngineOutput, TaskState, CognitiveScores, AssistanceMode, Severity, } from "./types";
export class AdaptRuleEngine {
    trace = [];
    /**
     * Main evaluation entry point.
     */
    evaluate(input) {
        this.trace = [];
        // Step 1: Determine task state
        const taskState = this.determineTaskState(input);
        this.trace.push(`→ Task state determined: ${taskState}`);
        // Step 2: Score cognitive difficulty
        const cognitiveScores = this.scoreCognitiveDifficulty(input);
        this.trace.push(`→ Cognitive scores: WM=${cognitiveScores.workingMemory}, ATN=${cognitiveScores.attention}, SPD=${cognitiveScores.processingSpeed}`);
        // Step 3: Determine primary and secondary issues
        const { primaryIssue, secondaryIssue } = this.identifyPrimaryIssues(cognitiveScores);
        this.trace.push(`→ Primary issue: ${primaryIssue}, Secondary: ${secondaryIssue}`);
        // Step 4: Severity and confidence
        const { severity, confidence } = this.assessSeverityAndConfidence(taskState, cognitiveScores, input.historySignals.recentFailureCount, input.taskContext.riskLevel);
        this.trace.push(`→ Severity: ${severity}, Confidence: ${confidence}%`);
        // Step 5: Select assistance mode
        const { assistanceMode, adaptationActions, escalationRequired, escalationReason } = this.selectAssistanceMode(taskState, severity, input.taskContext.riskLevel);
        this.trace.push(`→ Assistance mode: ${assistanceMode}`);
        if (escalationRequired) {
            this.trace.push(`→ ESCALATION REQUIRED: ${escalationReason}`);
        }
        return {
            taskState,
            cognitiveScores,
            primaryIssue,
            secondaryIssue,
            severity,
            confidence,
            assistanceMode,
            adaptationActions,
            escalationRequired,
            escalationReason,
            ruleTrace: this.trace,
        };
    }
    // ========================================================================
    // STEP 1: DETERMINE TASK STATE
    // ========================================================================
    determineTaskState(input) {
        const { passiveSignals, progressSignals, interactionSignals, taskContext } = input;
        // Completed?
        if (progressSignals.taskCompleted) {
            return "COMPLETED";
        }
        // Abandoned?
        if (progressSignals.taskAbandoned) {
            return "ABANDONED";
        }
        // Not started: reminder sent but no response for a while
        if (passiveSignals.reminderDelivered &&
            !passiveSignals.voicePromptAcknowledged &&
            !passiveSignals.motionDetectedAfterPrompt &&
            passiveSignals.noResponseDurationMs > 60000 // 1 minute
        ) {
            return "NOT_STARTED";
        }
        // Engaged: response or movement after prompt, but not yet progressing
        if ((passiveSignals.motionDetectedAfterPrompt || passiveSignals.devicePickedUp) &&
            progressSignals.currentStepIndex === 0) {
            return "ENGAGED";
        }
        // Progressing: making steady forward progress
        if (progressSignals.currentStepIndex > 0 &&
            progressSignals.stepRevisits <= 1 &&
            progressSignals.screenBacktracks === 0 &&
            progressSignals.idleTimeMs < taskContext.expectedDurationMs * 0.5) {
            return "PROGRESSING";
        }
        // Confused: many replays, revisits, backtracks
        if (interactionSignals.instructionReplay >= 2 ||
            progressSignals.stepRevisits >= 3 ||
            progressSignals.screenBacktracks >= 2) {
            return "CONFUSED";
        }
        // Stalled: progress exists but stuck on current step
        if (progressSignals.currentStepIndex > 0 &&
            !progressSignals.stepCompleted &&
            progressSignals.idleTimeMs > 30000) {
            return "STALLED";
        }
        // Default
        return "ENGAGED";
    }
    // ========================================================================
    // STEP 2: SCORE COGNITIVE DIFFICULTY (0–100 scale)
    // ========================================================================
    scoreCognitiveDifficulty(input) {
        const scores = {
            workingMemory: 0,
            attention: 0,
            processingSpeed: 0,
            decisionMaking: 0,
            comprehension: 0,
        };
        const { passiveSignals, progressSignals, interactionSignals, historySignals } = input;
        // ---- WORKING MEMORY ----
        // Indicated by: replays, revisits, forgotten steps
        if (interactionSignals.instructionReplay >= 1) {
            scores.workingMemory += 20;
            this.trace.push(`  • Instruction replay x${interactionSignals.instructionReplay} → WM +20`);
        }
        if (progressSignals.stepRevisits >= 2) {
            scores.workingMemory += 15;
            this.trace.push(`  • Step revisits x${progressSignals.stepRevisits} → WM +15`);
        }
        if (historySignals.recentMissedTaskCount >= 2) {
            scores.workingMemory += 10;
            this.trace.push(`  • Recent missed tasks x${historySignals.recentMissedTaskCount} → WM +10`);
        }
        // ---- ATTENTION ----
        // Indicated by: long inactivity, prompt ignoring, distractions
        if (passiveSignals.noResponseDurationMs > 90000) {
            scores.attention += 25;
            this.trace.push(`  • No response > 90s → ATN +25`);
        }
        if (historySignals.consecutivePromptIgnorals >= 1) {
            scores.attention += 15;
            this.trace.push(`  • Consecutive ignorials x${historySignals.consecutivePromptIgnorals} → ATN +15`);
        }
        if (progressSignals.idleTimeMs > 60000) {
            scores.attention += 10;
            this.trace.push(`  • Idle time > 60s → ATN +10`);
        }
        // ---- PROCESSING SPEED ----
        // Indicated by: slow response time, delays
        if (passiveSignals.noResponseDurationMs > 30000 &&
            passiveSignals.noResponseDurationMs <= 90000) {
            scores.processingSpeed += 15;
            this.trace.push(`  • Delayed response 30–90s → SPD +15`);
        }
        if (progressSignals.responseTimeMs > (historySignals.baselineResponseTimeMs * 1.5)) {
            scores.processingSpeed += 20;
            this.trace.push(`  • Response time >> baseline → SPD +20`);
        }
        // ---- DECISION MAKING ----
        // Indicated by: wrong choices, indecision, switching
        if (interactionSignals.wrongTapCount >= 2) {
            scores.decisionMaking += 15;
            this.trace.push(`  • Wrong taps x${interactionSignals.wrongTapCount} → DM +15`);
        }
        if (interactionSignals.choiceSwitches >= 1) {
            scores.decisionMaking += 10;
            this.trace.push(`  • Choice switches x${interactionSignals.choiceSwitches} → DM +10`);
        }
        if (interactionSignals.helpRequests >= 1) {
            scores.decisionMaking += 10;
            this.trace.push(`  • Help requests x${interactionSignals.helpRequests} → DM +10`);
        }
        // ---- COMPREHENSION ----
        // Indicated by: backtracks, repeated help, confusion
        if (progressSignals.screenBacktracks >= 1) {
            scores.comprehension += 15;
            this.trace.push(`  • Screen backtracks x${progressSignals.screenBacktracks} → CMP +15`);
        }
        if (historySignals.recentHelpFrequency > 50) {
            scores.comprehension += 20;
            this.trace.push(`  • High help frequency → CMP +20`);
        }
        // Cap all scores at 100
        Object.keys(scores).forEach((key) => {
            scores[key] = Math.min(100, scores[key]);
        });
        return scores;
    }
    // ========================================================================
    // STEP 3: IDENTIFY PRIMARY AND SECONDARY ISSUES
    // ========================================================================
    identifyPrimaryIssues(scores) {
        const sorted = Object.entries(scores)
            .sort(([, a], [, b]) => b - a)
            .map(([key]) => key);
        const primaryIssue = scores[sorted[0]] > 30 ? sorted[0] : "NONE";
        const secondaryIssue = scores[sorted[1]] > 20 ? sorted[1] : "NONE";
        return { primaryIssue, secondaryIssue };
    }
    // ========================================================================
    // STEP 4: ASSESS SEVERITY AND CONFIDENCE
    // ========================================================================
    assessSeverityAndConfidence(taskState, scores, recentFailureCount, taskRiskLevel) {
        let severity = "NONE";
        let confidence = 50; // default medium confidence
        const maxScore = Math.max(...Object.values(scores));
        // Severity rules
        if (taskState === "NOT_STARTED" || taskState === "ABANDONED") {
            severity = "CRITICAL";
            confidence = 90;
        }
        else if (taskState === "CONFUSED") {
            severity = maxScore > 60 ? "SEVERE" : "MODERATE";
            confidence = 80;
        }
        else if (taskState === "STALLED") {
            severity = maxScore > 50 ? "MODERATE" : "MILD";
            confidence = 70;
        }
        else if (taskState === "PROGRESSING") {
            severity = "NONE";
            confidence = 85;
        }
        // Boost confidence if there's a repeating pattern
        if (recentFailureCount >= 2) {
            confidence = Math.min(100, confidence + 15);
            this.trace.push(`  • Repeating pattern detected → confidence boosted to ${confidence}%`);
        }
        // High-risk tasks increase severity
        if (taskRiskLevel === "HIGH" && severity === "MILD") {
            severity = "MODERATE";
            this.trace.push(`  • High-risk task → severity upgraded to MODERATE`);
        }
        return { severity, confidence };
    }
    // ========================================================================
    // STEP 5: SELECT ASSISTANCE MODE
    // ========================================================================
    selectAssistanceMode(taskState, severity, taskRiskLevel) {
        let assistanceMode = "NONE";
        let adaptationActions = [];
        let escalationRequired = false;
        let escalationReason;
        // Decision tree based on task state + severity
        if (taskState === "PROGRESSING") {
            assistanceMode = "NONE";
            this.trace.push(`  ✓ Patient progressing normally, no intervention needed`);
        }
        else if (taskState === "NOT_STARTED") {
            if (taskRiskLevel === "HIGH") {
                assistanceMode = "ESCALATE_CARETAKER";
                escalationRequired = true;
                escalationReason = "Task not started and risk level is HIGH";
                adaptationActions = ["NOTIFY_CAREGIVER", "LOG_MISSED_TASK"];
            }
            else {
                assistanceMode = "SOFT_REMINDER";
                adaptationActions = ["SEND_REMINDER"];
            }
        }
        else if (taskState === "STALLED") {
            if (severity === "MODERATE" || severity === "SEVERE") {
                assistanceMode = "GUIDED_STEP";
                adaptationActions = ["SIMPLIFY_CURRENT_STEP", "ENABLE_VOICE", "EXTEND_TIMEOUT"];
            }
            else {
                assistanceMode = "VOICE_CUE";
                adaptationActions = ["SOFT_REMINDER", "VOICE_HINT"];
            }
        }
        else if (taskState === "CONFUSED") {
            if (severity === "SEVERE" || severity === "CRITICAL") {
                assistanceMode = "ESCALATE_CARETAKER";
                escalationRequired = true;
                escalationReason = "Patient confused, severity is SEVERE/CRITICAL";
                adaptationActions = ["ESCALATE_IMMEDIATELY", "LOG_CONFUSION_EVENT"];
            }
            else {
                assistanceMode = "GUIDED_STEP";
                adaptationActions = ["RESET_TO_LAST_GOOD_STEP", "SIMPLIFY_INSTRUCTIONS", "VOICE_ONLY"];
            }
        }
        else if (taskState === "ABANDONED") {
            assistanceMode = "ESCALATE_CARETAKER";
            escalationRequired = true;
            escalationReason = "Task abandoned by patient";
            adaptationActions = ["NOTIFY_CAREGIVER", "SCHEDULE_FOLLOWUP"];
        }
        return {
            assistanceMode,
            adaptationActions,
            escalationRequired,
            escalationReason,
        };
    }
}
//# sourceMappingURL=engine.js.map