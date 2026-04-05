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
  severity: string;
  assistanceMode: string;
  adaptationActions: any[];
  ruleTrace: string[];
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

      const result = await response.json();

      if (!result.success) {
        throw new Error(result.error || 'Engine evaluation failed');
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
      return await response.json();
    } catch (error) {
      console.error('Engine service health check failed:', error);
      return { status: 'UNAVAILABLE' };
    }
  },
};
