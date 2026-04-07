import { config } from '../config.js';

interface EngineInput {
  patientId: string;
  taskContext: {
    taskId: string;
    taskName: string;
    taskType: string;
    riskLevel: string;
  };
  passiveSignals?: any;
  interactionSignals?: any;
  progressSignals?: any;
  historySignals?: any;
}

interface EngineOutput {
  taskState: string;
  cognitiveScores: any;
  primaryIssue: string;
  secondaryIssue?: string;
  severity: string;
  confidence?: number;
  assistanceMode: string;
  adaptationActions: any[];
  escalationRequired?: boolean;
  escalationReason?: string;
  ruleTrace: string[];
}

interface EngineResponse {
  success: boolean;
  output?: EngineOutput;
  error?: string;
}

export const engineService = {
  async evaluate(input: EngineInput): Promise<EngineOutput> {
    try {
      const response = await fetch(`${config.engineService.url}/engine/evaluate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(input),
      });

      if (!response.ok) {
        throw new Error(`Engine service returned ${response.status}`);
      }

      const result = (await response.json()) as EngineResponse;

      if (!result.success) {
        throw new Error(result.error || 'Engine evaluation failed');
      }

      if (!result.output) {
        throw new Error('Engine evaluation failed');
      }

      return result.output;
    } catch (error) {
      console.error('Engine service error:', error);
      throw { status: 502, message: 'Engine service unavailable' };
    }
  },

  async health(): Promise<any> {
    try {
      const response = await fetch(`${config.engineService.url}/health`);

      if (!response.ok) {
        return {
          status: 'UNAVAILABLE',
          error: `HTTP_${response.status}`,
        };
      }

      const payload = await response.json();
      if (!payload || typeof payload !== 'object') {
        return {
          status: 'UNAVAILABLE',
          error: 'INVALID_HEALTH_PAYLOAD',
        };
      }

      return payload;
    } catch (error) {
      console.error('Engine service health check failed:', error);
      return { status: 'UNAVAILABLE' };
    }
  },
};
