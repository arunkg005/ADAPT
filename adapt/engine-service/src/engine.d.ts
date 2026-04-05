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
import { EngineInput, EngineOutput } from "./types";
export declare class AdaptRuleEngine {
    private trace;
    /**
     * Main evaluation entry point.
     */
    evaluate(input: EngineInput): EngineOutput;
    private determineTaskState;
    private scoreCognitiveDifficulty;
    private identifyPrimaryIssues;
    private assessSeverityAndConfidence;
    private selectAssistanceMode;
}
//# sourceMappingURL=engine.d.ts.map