/**
 * ADAPT Rule Engine - HTTP Server
 * 
 * Exposes the rule engine via REST API.
 * Can be called from:
 * - Android patient app (background service)
 * - Cloud backend (ingestion pipeline)
 * - Admin dashboard (simulation/debug)
 */

import express from "express";
import type { Express, Request, Response } from "express";
import cors from "cors";
import { AdaptRuleEngine } from "./engine.js";
import type { EngineInput, EngineEvaluationResult } from "./types.js";

const app: Express = express();
const port = Number(process.env.ENGINE_PORT || process.env.PORT || 4001);
const engine = new AdaptRuleEngine();

// Middleware
app.use(express.json());
app.use(cors());

// ============================================================================
// ROUTES
// ============================================================================

/**
 * Health check endpoint.
 */
app.get("/health", (req: Request, res: Response) => {
  res.json({
    status: "ok",
    service: "ADAPT Rule Engine",
    version: "0.1.0",
    timestamp: new Date().toISOString(),
  });
});

/**
 * Main evaluation endpoint.
 * 
 * POST /engine/evaluate
 * 
 * Request body: EngineInput (JSON)
 * Response: EngineEvaluationResult (JSON)
 * 
 * Example request:
 * {
 *   "patientId": "patient_001",
 *   "taskContext": {
 *     "taskId": "task_123",
 *     "taskName": "Take Medicine",
 *     "taskType": "MEDICATION",
 *     "scheduledStartTime": 1700000000,
 *     "expectedDurationMs": 300000,
 *     "stepCount": 3,
 *     "riskLevel": "HIGH",
 *     "complexity": "LOW"
 *   },
 *   "passiveSignals": {
 *     "reminderDelivered": true,
 *     "voicePromptDelivered": true,
 *     "voicePromptAcknowledged": false,
 *     "devicePickedUp": true,
 *     "motionDetectedAfterPrompt": true,
 *     "noResponseDurationMs": 90000,
 *     "taskWindowElapsedMs": 120000
 *   },
 *   "interactionSignals": {
 *     "singleTapConfirm": false,
 *     "voiceConfirmDone": false,
 *     "voiceHelpRequest": true,
 *     "instructionReplay": 2,
 *     "promptDismissed": 0,
 *     "wrongTapCount": 0,
 *     "choiceSwitches": 0,
 *     "helpRequests": 1
 *   },
 *   "progressSignals": {
 *     "currentStepIndex": 1,
 *     "stepCompleted": false,
 *     "stepRevisits": 2,
 *     "screenBacktracks": 0,
 *     "taskCompleted": false,
 *     "taskAbandoned": false,
 *     "idleTimeMs": 30000,
 *     "responseTimeMs": 45000
 *   },
 *   "historySignals": {
 *     "recentFailureCount": 2,
 *     "recentMissedTaskCount": 1,
 *     "recentHelpFrequency": 65,
 *     "baselineResponseTimeMs": 15000,
 *     "commonDifficultyType": "COMPREHENSION",
 *     "lastAdaptationUsed": "VOICE_GUIDED",
 *     "lastAdaptationSuccess": true,
 *     "consecutivePromptIgnorals": 0
 *   }
 * }
 */
app.post("/engine/evaluate", (req: Request, res: Response) => {
  try {
    const input = req.body as EngineInput;

    // Validation: check required fields
    if (!input.patientId || !input.taskContext || !input.passiveSignals || 
        !input.interactionSignals || !input.progressSignals || !input.historySignals) {
      const result: EngineEvaluationResult = {
        success: false,
        error: "Missing required fields in request body",
      };
      return res.status(400).json(result);
    }

    // Run the engine
    const output = engine.evaluate(input);

    const result: EngineEvaluationResult = {
      success: true,
      output,
    };

    res.json(result);
  } catch (error) {
    console.error("Engine evaluation error:", error);
    const result: EngineEvaluationResult = {
      success: false,
      error: error instanceof Error ? error.message : "Unknown error",
    };
    res.status(500).json(result);
  }
});

/**
 * Documentation endpoint.
 */
app.get("/docs", (req: Request, res: Response) => {
  res.json({
    service: "ADAPT Rule Engine",
    description: "Behavioral assessment engine for cognitively vulnerable users",
    endpoints: {
      health: {
        method: "GET",
        path: "/health",
        description: "Service health check",
      },
      evaluate: {
        method: "POST",
        path: "/engine/evaluate",
        description: "Evaluate task behavior and return assistance recommendation",
        input: "EngineInput (see /engine/evaluate POST for example)",
        output: "EngineEvaluationResult with EngineOutput",
      },
      docs: {
        method: "GET",
        path: "/docs",
        description: "This documentation",
      },
    },
  });
});

// ============================================================================
// ERROR HANDLING
// ============================================================================

app.use((req: Request, res: Response) => {
  res.status(404).json({
    error: "Not found",
    path: req.path,
  });
});

// ============================================================================
// SERVER START
// ============================================================================

app.listen(port, () => {
  console.log(`
╔════════════════════════════════════════════════════════════════╗
║              ADAPT Rule Engine Service                         ║
║                                                                ║
║  Server running on http://localhost:${port}                    ║
║  Health: http://localhost:${port}/health                       ║
║  Docs:   http://localhost:${port}/docs                         ║
║                                                                ║
║  POST /engine/evaluate  →  Behavioral assessment              ║
╚════════════════════════════════════════════════════════════════╝
  `);
});
