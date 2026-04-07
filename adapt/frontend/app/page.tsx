"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

type UserRole = "ADMIN" | "CAREGIVER" | "PATIENT";

interface AuthUser {
  id: string;
  email: string;
  role: UserRole;
  first_name?: string;
  last_name?: string;
}

interface AuthResponse {
  token: string;
  user: AuthUser;
}

interface TaskTemplate {
  key: string;
  name: string;
  description: string;
  task_type: string;
  risk_level: string;
  complexity: string;
  default_time: string;
  steps: Array<{ title: string; details: string }>;
}

interface TaskPlanStep {
  id?: string;
  step_order: number;
  title: string;
  details?: string;
  is_required: boolean;
  is_completed?: boolean;
}

interface TaskPlan {
  id: string;
  title: string;
  description?: string;
  scheduled_time?: string;
  task_type: string;
  risk_level: string;
  complexity: string;
  status: string;
  source: string;
  created_at: string;
  steps: TaskPlanStep[];
}

interface TaskDraftResponse {
  message: string;
  guidance: string;
  draft: {
    title: string;
    description?: string;
    scheduled_time?: string;
    task_type?: string;
    risk_level?: string;
    complexity?: string;
    status?: string;
    source?: string;
    template_key?: string;
    steps?: TaskPlanStep[];
  };
}

interface OverviewResponse {
  window: {
    fromMs: number;
    toMs: number;
  };
  totals: {
    patients: number;
    alerts: number;
    unacknowledgedAlerts: number;
    telemetrySignals: number;
  };
  taskLab: {
    byStatus: Record<string, number>;
  };
}

interface PatientSummaryResponse {
  patient: {
    id: string;
    name: string;
    riskLevel: string;
  };
  window: {
    fromMs: number;
    toMs: number;
  };
  telemetry: {
    total: number;
    byType: Array<{ signalType: string; count: number }>;
  };
  alerts: {
    total: number;
    unacknowledged: number;
    bySeverity: Record<string, number>;
  };
  taskLab: {
    byStatus: Record<string, number>;
  };
  insight: {
    dominantIssue: string;
    trend: string;
    recommendation: string;
  };
}

interface ChatResponse {
  reply: string;
  actionItems: string[];
  safetyFlags: string[];
  confidence: number;
}

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:3001/api";
const TOKEN_KEY = "adapt_web_token";
const USER_KEY = "adapt_web_user";

const parseError = async (response: Response): Promise<string> => {
  const payload = (await response.json().catch(() => ({}))) as { error?: string };
  if (payload.error && payload.error.trim().length > 0) {
    return payload.error;
  }

  return `Request failed (${response.status})`;
};

async function apiRequest<T>(
  path: string,
  token: string | null,
  options?: {
    method?: "GET" | "POST" | "PUT" | "DELETE";
    body?: unknown;
  }
): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: options?.method || "GET",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: options?.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (!response.ok) {
    throw new Error(await parseError(response));
  }

  return (await response.json()) as T;
}

const labelCase = (value: string): string => {
  return value
    .toLowerCase()
    .replace(/_/g, " ")
    .replace(/\b\w/g, (match) => match.toUpperCase());
};

export default function Home() {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);

  const [authMode, setAuthMode] = useState<"login" | "register">("login");
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");

  const [activeTab, setActiveTab] = useState<"tasklab" | "analysis" | "chat">("tasklab");

  const [activePatientId, setActivePatientId] = useState("");
  const [taskPrompt, setTaskPrompt] = useState("");
  const [generatedDraft, setGeneratedDraft] = useState<TaskDraftResponse | null>(null);
  const [taskStatus, setTaskStatus] = useState("");
  const [taskBusy, setTaskBusy] = useState(false);
  const [templates, setTemplates] = useState<TaskTemplate[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [taskPlans, setTaskPlans] = useState<TaskPlan[]>([]);

  const [analysisWindowHours, setAnalysisWindowHours] = useState("24");
  const [overview, setOverview] = useState<OverviewResponse | null>(null);
  const [summary, setSummary] = useState<PatientSummaryResponse | null>(null);
  const [analysisStatus, setAnalysisStatus] = useState("");
  const [analysisBusy, setAnalysisBusy] = useState(false);

  const [chatPrompt, setChatPrompt] = useState("");
  const [chatBusy, setChatBusy] = useState(false);
  const [chatMessages, setChatMessages] = useState<string[]>([]);

  const caregiverName = useMemo(() => {
    if (!user) {
      return "Caregiver";
    }

    const joined = `${user.first_name || ""} ${user.last_name || ""}`.trim();
    if (joined.length > 0) {
      return joined;
    }

    return user.email.split("@")[0] || "Caregiver";
  }, [user]);

  const persistSession = (payload: AuthResponse) => {
    setToken(payload.token);
    setUser(payload.user);
    localStorage.setItem(TOKEN_KEY, payload.token);
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user));
  };

  const clearSession = () => {
    setToken(null);
    setUser(null);
    setTaskPlans([]);
    setGeneratedDraft(null);
    setSummary(null);
    setOverview(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  };

  const loadTemplates = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      const response = await apiRequest<{ data: TaskTemplate[] }>("/task-lab/templates", token);
      setTemplates(response.data || []);
      if (response.data.length > 0) {
        setSelectedTemplate((currentTemplate) => currentTemplate || response.data[0].key);
      }
    } catch (error) {
      setTaskStatus(error instanceof Error ? error.message : "Failed to load templates.");
    }
  }, [token]);

  const loadTaskPlans = useCallback(
    async (patientId: string) => {
      if (!token) {
        return;
      }

      try {
        const response = await apiRequest<{ data: TaskPlan[] }>(
          `/task-lab/plans?patientId=${encodeURIComponent(patientId)}&limit=50&offset=0`,
          token
        );
        setTaskPlans(response.data || []);
      } catch (error) {
        setTaskStatus(error instanceof Error ? error.message : "Failed to load plans.");
      }
    },
    [token]
  );

  const loadOverview = useCallback(async () => {
    if (!token) {
      return;
    }

    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const response = await apiRequest<OverviewResponse>(`/analysis/overview?windowMs=${windowMs}`, token);
      setOverview(response);
    } catch (error) {
      setAnalysisStatus(error instanceof Error ? error.message : "Unable to load overview.");
    }
  }, [token, analysisWindowHours]);

  useEffect(() => {
    const savedToken = localStorage.getItem(TOKEN_KEY);
    const savedUserRaw = localStorage.getItem(USER_KEY);

    if (!savedToken || !savedUserRaw) {
      return;
    }

    try {
      const savedUser = JSON.parse(savedUserRaw) as AuthUser;
      setToken(savedToken);
      setUser(savedUser);
    } catch {
      clearSession();
    }
  }, []);

  useEffect(() => {
    if (!token) {
      return;
    }

    void loadTemplates();
  }, [token, loadTemplates]);

  useEffect(() => {
    if (!token) {
      return;
    }

    void loadOverview();
  }, [token, loadOverview]);

  useEffect(() => {
    if (!token || activePatientId.trim().length === 0) {
      setTaskPlans([]);
      return;
    }

    void loadTaskPlans(activePatientId.trim());
  }, [token, activePatientId, loadTaskPlans]);

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError("");

    try {
      const response = await apiRequest<AuthResponse>("/auth/login", null, {
        method: "POST",
        body: {
          email,
          password,
          platform: "WEB",
        },
      });

      persistSession(response);
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : "Unable to sign in.");
    } finally {
      setAuthLoading(false);
    }
  };

  const handleRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthLoading(true);
    setAuthError("");

    try {
      const response = await apiRequest<AuthResponse>("/auth/register", null, {
        method: "POST",
        body: {
          email,
          password,
          first_name: firstName,
          last_name: lastName,
          role: "CAREGIVER",
          platform: "WEB",
        },
      });

      persistSession(response);
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : "Unable to create account.");
    } finally {
      setAuthLoading(false);
    }
  };

  const generateTaskDraft = async () => {
    if (!token) {
      return;
    }

    if (taskPrompt.trim().length === 0) {
      setTaskStatus("Enter a prompt first.");
      return;
    }

    setTaskBusy(true);
    setTaskStatus("");

    try {
      const response = await apiRequest<TaskDraftResponse>("/ai/task-lab/generate", token, {
        method: "POST",
        body: {
          prompt: taskPrompt,
        },
      });

      setGeneratedDraft(response);
      setTaskStatus("AI draft generated. Review and publish to Task Lab.");
    } catch (error) {
      setTaskStatus(error instanceof Error ? error.message : "Draft generation failed.");
    } finally {
      setTaskBusy(false);
    }
  };

  const createTemplatePlan = async () => {
    if (!token) {
      return;
    }

    if (activePatientId.trim().length === 0) {
      setTaskStatus("Set patient ID before creating template plans.");
      return;
    }

    if (!selectedTemplate) {
      setTaskStatus("Select a template first.");
      return;
    }

    setTaskBusy(true);
    setTaskStatus("");

    try {
      await apiRequest<TaskPlan>("/task-lab/plans/from-template", token, {
        method: "POST",
        body: {
          patient_id: activePatientId.trim(),
          template_key: selectedTemplate,
        },
      });

      setTaskStatus("Template plan created.");
      await loadTaskPlans(activePatientId.trim());
    } catch (error) {
      setTaskStatus(error instanceof Error ? error.message : "Template creation failed.");
    } finally {
      setTaskBusy(false);
    }
  };

  const publishGeneratedDraft = async () => {
    if (!token || !generatedDraft) {
      return;
    }

    if (activePatientId.trim().length === 0) {
      setTaskStatus("Set patient ID before publishing a draft.");
      return;
    }

    setTaskBusy(true);
    setTaskStatus("");

    try {
      await apiRequest<TaskPlan>("/task-lab/plans", token, {
        method: "POST",
        body: {
          patient_id: activePatientId.trim(),
          title: generatedDraft.draft.title,
          description: generatedDraft.draft.description,
          scheduled_time: generatedDraft.draft.scheduled_time,
          task_type: generatedDraft.draft.task_type,
          risk_level: generatedDraft.draft.risk_level,
          complexity: generatedDraft.draft.complexity,
          status: "DRAFT",
          source: generatedDraft.draft.source || "AI",
          template_key: generatedDraft.draft.template_key,
          steps: (generatedDraft.draft.steps || []).map((step, index) => ({
            step_order: step.step_order || index + 1,
            title: step.title,
            details: step.details,
            is_required: step.is_required !== false,
          })),
        },
      });

      setTaskStatus("Draft saved to Task Lab.");
      await loadTaskPlans(activePatientId.trim());
    } catch (error) {
      setTaskStatus(error instanceof Error ? error.message : "Unable to publish draft.");
    } finally {
      setTaskBusy(false);
    }
  };

  const publishPlan = async (planId: string) => {
    if (!token) {
      return;
    }

    setTaskBusy(true);
    setTaskStatus("");

    try {
      await apiRequest<TaskPlan>(`/task-lab/plans/${planId}/status`, token, {
        method: "PUT",
        body: { status: "PUBLISHED" },
      });

      setTaskStatus("Plan status updated to Published.");
      if (activePatientId.trim().length > 0) {
        await loadTaskPlans(activePatientId.trim());
      }
    } catch (error) {
      setTaskStatus(error instanceof Error ? error.message : "Failed to update plan status.");
    } finally {
      setTaskBusy(false);
    }
  };

  const loadPatientSummary = async () => {
    if (!token) {
      return;
    }

    if (activePatientId.trim().length === 0) {
      setAnalysisStatus("Set patient ID before loading summary.");
      return;
    }

    setAnalysisBusy(true);
    setAnalysisStatus("");

    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const response = await apiRequest<PatientSummaryResponse>(
        `/analysis/patient/${encodeURIComponent(activePatientId.trim())}/summary?windowMs=${windowMs}`,
        token
      );
      setSummary(response);
    } catch (error) {
      setAnalysisStatus(error instanceof Error ? error.message : "Unable to load patient summary.");
    } finally {
      setAnalysisBusy(false);
    }
  };

  const sendChat = async () => {
    if (!token) {
      return;
    }

    if (chatPrompt.trim().length === 0) {
      return;
    }

    setChatBusy(true);

    try {
      const payload = await apiRequest<ChatResponse>("/ai/chat", token, {
        method: "POST",
        body: {
          prompt: chatPrompt,
          patientId: activePatientId.trim() || undefined,
          roleContext: "CAREGIVER",
        },
      });

      const actionLines = payload.actionItems.map((item) => `- ${item}`).join("\n");
      const flags = payload.safetyFlags.length > 0 ? payload.safetyFlags.join(", ") : "None";
      setChatMessages((prev) => [
        ...prev,
        `You: ${chatPrompt}`,
        `Assistant: ${payload.reply}\nActions:\n${actionLines}\nSafety Flags: ${flags}`,
      ]);
      setChatPrompt("");
    } catch (error) {
      setChatMessages((prev) => [
        ...prev,
        `System: ${error instanceof Error ? error.message : "Unable to contact AI endpoint."}`,
      ]);
    } finally {
      setChatBusy(false);
    }
  };

  if (!token || !user) {
    return (
      <main className="page-root">
        <section className="hero-panel">
          <p className="eyebrow">ADAPT Caregiver Console</p>
          <h1>Caregiver-only planning and analysis workspace</h1>
          <p>
            This web app is restricted to caregiver and admin accounts. Patient-facing assist experiences remain in mobile Assist Mode.
          </p>
        </section>

        <section className="auth-panel">
          <h2>{authMode === "login" ? "Sign in" : "Create caregiver account"}</h2>

          <form className="stack" onSubmit={authMode === "login" ? handleLogin : handleRegister}>
            {authMode === "register" && (
              <>
                <input
                  placeholder="First name"
                  value={firstName}
                  onChange={(event) => setFirstName(event.target.value)}
                  required
                />
                <input
                  placeholder="Last name"
                  value={lastName}
                  onChange={(event) => setLastName(event.target.value)}
                />
              </>
            )}

            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
            <input
              type="password"
              placeholder="Password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              required
            />

            {authError && <p className="error-text">{authError}</p>}

            <button type="submit" disabled={authLoading}>
              {authLoading ? "Please wait..." : authMode === "login" ? "Sign In" : "Create Account"}
            </button>
          </form>

          <button
            className="ghost"
            type="button"
            onClick={() => {
              setAuthMode(authMode === "login" ? "register" : "login");
              setAuthError("");
            }}
          >
            {authMode === "login" ? "Need a caregiver account? Register" : "Already have an account? Sign in"}
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard-root">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">ADAPT Unified Planner</p>
          <h1>Welcome, {caregiverName}</h1>
          <p className="muted">Role: {labelCase(user.role)}</p>
        </div>
        <button className="danger" onClick={clearSession}>
          Sign out
        </button>
      </header>

      <section className="filters-row">
        <label>
          Active patient ID
          <input
            value={activePatientId}
            onChange={(event) => setActivePatientId(event.target.value)}
            placeholder="Paste patient UUID"
          />
        </label>
        <label>
          Analysis window (hours)
          <input
            value={analysisWindowHours}
            onChange={(event) => setAnalysisWindowHours(event.target.value)}
            placeholder="24"
          />
        </label>
      </section>

      <nav className="tab-row">
        <button className={activeTab === "tasklab" ? "tab active" : "tab"} onClick={() => setActiveTab("tasklab")}>Task Lab</button>
        <button className={activeTab === "analysis" ? "tab active" : "tab"} onClick={() => setActiveTab("analysis")}>Analysis</button>
        <button className={activeTab === "chat" ? "tab active" : "tab"} onClick={() => setActiveTab("chat")}>AI Assistant</button>
      </nav>

      {activeTab === "tasklab" && (
        <section className="grid-layout">
          <article className="panel">
            <h3>Template-powered Todo Generator</h3>
            <p className="muted">Create sophisticated plans quickly from predefined templates.</p>
            <select value={selectedTemplate} onChange={(event) => setSelectedTemplate(event.target.value)}>
              {templates.map((template) => (
                <option key={template.key} value={template.key}>
                  {template.name} ({template.risk_level})
                </option>
              ))}
            </select>
            <button disabled={taskBusy} onClick={createTemplatePlan}>
              Use Template
            </button>
          </article>

          <article className="panel">
            <h3>AI Task Generation</h3>
            <textarea
              value={taskPrompt}
              onChange={(event) => setTaskPrompt(event.target.value)}
              placeholder="Example: create a low-complexity morning medication and hydration routine"
              rows={4}
            />
            <div className="row-actions">
              <button disabled={taskBusy} onClick={generateTaskDraft}>
                {taskBusy ? "Generating..." : "Generate Draft"}
              </button>
              <button className="ghost" disabled={taskBusy || !generatedDraft} onClick={publishGeneratedDraft}>
                Save Draft
              </button>
            </div>

            {generatedDraft && (
              <div className="draft-box">
                <p className="eyebrow">{generatedDraft.draft.title}</p>
                <p>{generatedDraft.draft.description}</p>
                <p className="muted">{generatedDraft.guidance}</p>
                <ul>
                  {(generatedDraft.draft.steps || []).map((step) => (
                    <li key={`${step.step_order}-${step.title}`}>
                      {step.step_order}. {step.title}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {taskStatus && <p className="status-text">{taskStatus}</p>}
          </article>

          <article className="panel span-2">
            <h3>Patient Task Plans</h3>
            <p className="muted">Shared backend plans synced across web and mobile task workflows.</p>
            <div className="plan-list">
              {taskPlans.length === 0 && <p className="muted">No plans found for this patient ID yet.</p>}
              {taskPlans.map((plan) => (
                <div className="plan-item" key={plan.id}>
                  <div>
                    <p className="plan-title">{plan.title}</p>
                    <p className="muted">{plan.description || "No description"}</p>
                    <p className="muted">
                      {labelCase(plan.status)} | {labelCase(plan.source)} | {plan.scheduled_time || "No time"}
                    </p>
                  </div>
                  <div className="row-actions">
                    {plan.status !== "PUBLISHED" && (
                      <button className="ghost" onClick={() => publishPlan(plan.id)}>
                        Publish
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </article>
        </section>
      )}

      {activeTab === "analysis" && (
        <section className="grid-layout">
          <article className="panel">
            <h3>Overview</h3>
            <button className="ghost" onClick={loadOverview}>
              Refresh Overview
            </button>
            {overview ? (
              <ul>
                <li>Patients: {overview.totals.patients}</li>
                <li>Alerts: {overview.totals.alerts}</li>
                <li>Unacknowledged: {overview.totals.unacknowledgedAlerts}</li>
                <li>Telemetry Signals: {overview.totals.telemetrySignals}</li>
              </ul>
            ) : (
              <p className="muted">No overview loaded yet.</p>
            )}
          </article>

          <article className="panel span-2">
            <h3>Patient Summary</h3>
            <button disabled={analysisBusy} onClick={loadPatientSummary}>
              {analysisBusy ? "Loading..." : "Load Summary"}
            </button>

            {summary && (
              <div className="summary-grid">
                <div className="metric-card">
                  <p className="eyebrow">Patient</p>
                  <h4>{summary.patient.name}</h4>
                  <p className="muted">Risk: {labelCase(summary.patient.riskLevel)}</p>
                </div>
                <div className="metric-card">
                  <p className="eyebrow">Telemetry</p>
                  <h4>{summary.telemetry.total}</h4>
                  <p className="muted">Signals in selected window</p>
                </div>
                <div className="metric-card">
                  <p className="eyebrow">Alerts</p>
                  <h4>{summary.alerts.unacknowledged}</h4>
                  <p className="muted">Unacknowledged of {summary.alerts.total}</p>
                </div>
                <div className="metric-card">
                  <p className="eyebrow">Trend</p>
                  <h4>{labelCase(summary.insight.trend)}</h4>
                  <p className="muted">{summary.insight.recommendation}</p>
                </div>
              </div>
            )}

            {analysisStatus && <p className="status-text">{analysisStatus}</p>}
          </article>
        </section>
      )}

      {activeTab === "chat" && (
        <section className="grid-layout">
          <article className="panel span-3">
            <h3>AI Care Assistant</h3>
            <p className="muted">Use this for care-planning guidance and risk-oriented action suggestions.</p>
            <textarea
              value={chatPrompt}
              onChange={(event) => setChatPrompt(event.target.value)}
              placeholder="Ask for guidance, triage strategy, or planning recommendations"
              rows={3}
            />
            <button disabled={chatBusy} onClick={sendChat}>
              {chatBusy ? "Sending..." : "Send"}
            </button>

            <div className="chat-box">
              {chatMessages.length === 0 && <p className="muted">Conversation appears here.</p>}
              {chatMessages.map((message, index) => (
                <pre key={index}>{message}</pre>
              ))}
            </div>
          </article>
        </section>
      )}
    </main>
  );
}
