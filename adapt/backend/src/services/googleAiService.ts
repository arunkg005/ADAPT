import { config } from '../config.js';

type GenericRecord = Record<string, any>;

interface ConversationMessage {
  role: string;
  content: string;
}

interface ChatGenerationInput {
  prompt: string;
  roleContext?: string;
  conversationHistory?: ConversationMessage[];
}

interface TaskGenerationInput {
  prompt: string;
  patientProfile?: GenericRecord;
  constraints?: GenericRecord;
}

const GOOGLE_API_BASE_URL = 'https://generativelanguage.googleapis.com/v1beta/models';

const normalizeText = (value: unknown): string => String(value || '').trim();

const toRole = (value: unknown): 'user' | 'model' => {
  const role = normalizeText(value).toLowerCase();
  return role === 'assistant' || role === 'model' || role === 'ai' ? 'model' : 'user';
};

const parsePositiveInt = (value: unknown, fallback: number): number => {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }

  return Math.floor(parsed);
};

const requestWithTimeout = async (url: string, options: RequestInit, timeoutMs: number): Promise<Response> => {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal,
    });
  } catch (error: any) {
    if (error?.name === 'AbortError') {
      throw { status: 504, message: 'Gemini request timed out' };
    }

    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
};

const extractTextFromResponse = (payload: GenericRecord): string => {
  const candidates = Array.isArray(payload?.candidates) ? payload.candidates : [];
  const firstCandidate = candidates[0];
  const parts = Array.isArray(firstCandidate?.content?.parts) ? firstCandidate.content.parts : [];

  return parts
    .map((part: GenericRecord) => (typeof part?.text === 'string' ? part.text.trim() : ''))
    .filter((text: string) => text.length > 0)
    .join('\n')
    .trim();
};

const extractJsonObject = (text: string): GenericRecord | null => {
  if (!text) {
    return null;
  }

  const directParse = (() => {
    try {
      const parsed = JSON.parse(text);
      return parsed && typeof parsed === 'object' ? (parsed as GenericRecord) : null;
    } catch {
      return null;
    }
  })();

  if (directParse) {
    return directParse;
  }

  const fencedMatch = text.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (fencedMatch?.[1]) {
    try {
      const parsed = JSON.parse(fencedMatch[1]);
      return parsed && typeof parsed === 'object' ? (parsed as GenericRecord) : null;
    } catch {
      // Continue to best-effort object slicing.
    }
  }

  const firstBrace = text.indexOf('{');
  const lastBrace = text.lastIndexOf('}');
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    try {
      const parsed = JSON.parse(text.slice(firstBrace, lastBrace + 1));
      return parsed && typeof parsed === 'object' ? (parsed as GenericRecord) : null;
    } catch {
      return null;
    }
  }

  return null;
};

const safeJsonStringify = (value: unknown): string => {
  try {
    return JSON.stringify(value ?? {}, null, 2);
  } catch {
    return '{}';
  }
};

const buildConversationContents = (conversationHistory?: ConversationMessage[]): GenericRecord[] => {
  const history = Array.isArray(conversationHistory) ? conversationHistory : [];
  const contents: GenericRecord[] = [];

  for (const entry of history) {
    const content = normalizeText(entry?.content);
    if (!content) {
      continue;
    }

    contents.push({
      role: toRole(entry?.role),
      parts: [{ text: content }],
    });
  }

  return contents;
};

const callJsonModel = async (params: {
  prompt: string;
  systemInstruction: string;
  conversationHistory?: ConversationMessage[];
  temperature: number;
}): Promise<GenericRecord> => {
  const apiKey = normalizeText(config.gemini.apiKey);
  if (!apiKey) {
    throw { status: 503, message: 'GEMINI_API_KEY is not configured' };
  }

  const model = normalizeText(config.gemini.model) || 'gemini-2.0-flash';
  const timeoutMs = parsePositiveInt(config.gemini.timeoutMs, 15000);
  const maxOutputTokens = parsePositiveInt(config.gemini.maxOutputTokens, 1024);

  const url = `${GOOGLE_API_BASE_URL}/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;

  const body: GenericRecord = {
    contents: [
      ...buildConversationContents(params.conversationHistory),
      {
        role: 'user',
        parts: [{ text: params.prompt }],
      },
    ],
    generationConfig: {
      temperature: params.temperature,
      maxOutputTokens,
      responseMimeType: 'application/json',
    },
  };

  const systemInstruction = normalizeText(params.systemInstruction);
  if (systemInstruction) {
    body.systemInstruction = {
      parts: [{ text: systemInstruction }],
    };
  }

  const response = await requestWithTimeout(
    url,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    },
    timeoutMs
  );

  if (!response.ok) {
    let details = '';
    try {
      const errorPayload = (await response.json()) as GenericRecord;
      details = normalizeText(errorPayload?.error?.message || errorPayload?.message);
    } catch {
      details = '';
    }

    const suffix = details ? `: ${details}` : '';
    throw {
      status: 502,
      message: `Gemini request failed (${response.status})${suffix}`,
    };
  }

  const payload = (await response.json()) as GenericRecord;
  const text = extractTextFromResponse(payload);
  const parsed = extractJsonObject(text);

  if (!parsed) {
    throw { status: 502, message: 'Gemini returned invalid JSON response' };
  }

  return parsed;
};

const buildSystemInstruction = (scope: 'chat' | 'task'): string => {
  const base =
    scope === 'chat'
      ? 'You are ADAPT Care AI for caregiver decision support. Prioritize practical, safe, and concise guidance.'
      : 'You are ADAPT Care AI for routine planning. Generate actionable, risk-aware task drafts suitable for caregiver workflows.';

  const extra = normalizeText(config.gemini.systemInstruction);
  return extra ? `${base}\n\n${extra}` : base;
};

export const googleAiService = {
  isConfigured(): boolean {
    return normalizeText(config.gemini.apiKey).length > 0;
  },

  async generateChat(payload: ChatGenerationInput): Promise<GenericRecord> {
    const roleContext = normalizeText(payload.roleContext || 'CAREGIVER');
    const prompt = normalizeText(payload.prompt);

    const modelPrompt = [
      'Return ONLY JSON with this exact schema:',
      '{',
      '  "reply": "string",',
      '  "actionItems": ["string"],',
      '  "safetyFlags": ["UPPER_SNAKE_CASE"],',
      '  "confidence": 0.0',
      '}',
      'Rules:',
      '- reply should be concise and actionable for a caregiver.',
      '- actionItems should include 2 to 4 concrete next steps.',
      '- safetyFlags should contain zero or more tags (for example FALL_RISK, MEDICATION_ADHERENCE).',
      '- confidence must be a number between 0 and 1.',
      `- roleContext: ${roleContext}`,
      '',
      `User message: ${prompt}`,
    ].join('\n');

    return callJsonModel({
      prompt: modelPrompt,
      systemInstruction: buildSystemInstruction('chat'),
      conversationHistory: payload.conversationHistory,
      temperature: 0.2,
    });
  },

  async generateTaskDraft(payload: TaskGenerationInput): Promise<GenericRecord> {
    const prompt = normalizeText(payload.prompt);

    const modelPrompt = [
      'Return ONLY JSON with this exact schema:',
      '{',
      '  "guidance": "string",',
      '  "draft": {',
      '    "title": "string",',
      '    "description": "string",',
      '    "scheduled_time": "HH:MM AM/PM",',
      '    "task_type": "MEDICATION|HYGIENE|MEAL|EXERCISE|SOCIAL|OTHER",',
      '    "risk_level": "LOW|MEDIUM|HIGH",',
      '    "complexity": "LOW|MEDIUM|HIGH",',
      '    "steps": [',
      '      { "title": "string", "details": "string", "is_required": true }',
      '    ]',
      '  }',
      '}',
      'Rules:',
      '- Provide 3 to 6 steps in order of execution.',
      '- Keep all text caregiver-friendly and specific.',
      '- Respect constraints and patient profile when provided.',
      '',
      'Patient profile JSON:',
      safeJsonStringify(payload.patientProfile),
      '',
      'Constraints JSON:',
      safeJsonStringify(payload.constraints),
      '',
      `Prompt: ${prompt}`,
    ].join('\n');

    return callJsonModel({
      prompt: modelPrompt,
      systemInstruction: buildSystemInstruction('task'),
      temperature: 0.25,
    });
  },
};