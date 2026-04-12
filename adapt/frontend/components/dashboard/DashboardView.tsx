/* eslint-disable @typescript-eslint/no-explicit-any */

import { useCallback, useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import DashboardHeader from "./DashboardHeader";
import KpiRow from "./KpiRow";
import TaskLabPanel from "./TaskLabPanel";
import AnalysisPanel from "./AnalysisPanel";
import ChatPanel from "./ChatPanel";
import PatientsPanel from "./PatientsPanel";
import SettingsPanel from "./SettingsPanel";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface AuthUser {
  id: string;
  email: string;
  role?: string;
  first_name?: string;
  last_name?: string;
}

interface DashboardViewProps {
  token: string;
  user: AuthUser;
  apiBase: string;
  onLogout: () => void;
}

interface Patient {
  id: string;
  first_name: string;
  last_name: string;
  date_of_birth?: string;
  cognitive_condition?: string;
  risk_level?: string;
}

interface PatientFormState {
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  cognitiveCondition: string;
  riskLevel: string;
  baselineResponseTimeMs: string;
}

interface DashboardSettings {
  defaultPatientId: string;
  analysisWindowHours: string;
  autoRefreshOverview: boolean;
  refreshIntervalMinutes: string;
}

type ChatMessageRole = "user" | "assistant" | "system";

interface ChatMessage {
  id: string;
  role: ChatMessageRole;
  text: string;
  actionItems?: string[];
  safetyFlags?: string[];
  confidence?: number;
  timestamp: number;
}

type DashboardTab = "patients" | "tasklab" | "analysis" | "chat" | "settings";

async function apiRequest<T>(apiBase: string, path: string, token: string, options?: { method?: string; body?: unknown }): Promise<T> {
  const res = await fetch(`${apiBase}${path}`, {
    method: options?.method || "GET",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: options?.body ? JSON.stringify(options.body) : undefined,
  });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    throw new Error((data as { error?: string }).error || `Request failed (${res.status})`);
  }
  return res.json();
}

const DASHBOARD_SETTINGS_KEY = "adapt_dashboard_settings";

const DashboardView = ({ token, user, apiBase, onLogout }: DashboardViewProps) => {
  const [activeTab, setActiveTab] = useState<DashboardTab>("patients");
  const [activePatientId, setActivePatientId] = useState("");
  const [analysisWindowHours, setAnalysisWindowHours] = useState("24");

  // Patients state
  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientBusy, setPatientBusy] = useState(false);
  const [patientStatus, setPatientStatus] = useState("");
  const [patientForm, setPatientForm] = useState<PatientFormState>({
    firstName: "",
    lastName: "",
    dateOfBirth: "",
    cognitiveCondition: "",
    riskLevel: "MEDIUM",
    baselineResponseTimeMs: "3000",
  });

  // Settings state
  const [settingsForm, setSettingsForm] = useState<DashboardSettings>({
    defaultPatientId: "",
    analysisWindowHours: "24",
    autoRefreshOverview: false,
    refreshIntervalMinutes: "5",
  });
  const [settingsStatus, setSettingsStatus] = useState("");

  // Task Lab state
  const [taskPrompt, setTaskPrompt] = useState("");
  const [generatedDraft, setGeneratedDraft] = useState<any>(null);
  const [taskStatus, setTaskStatus] = useState("");
  const [taskBusy, setTaskBusy] = useState(false);
  const [templates, setTemplates] = useState<any[]>([]);
  const [selectedTemplate, setSelectedTemplate] = useState("");
  const [taskPlans, setTaskPlans] = useState<any[]>([]);

  // Analysis state
  const [overview, setOverview] = useState<any>(null);
  const [summary, setSummary] = useState<any>(null);
  const [analysisStatus, setAnalysisStatus] = useState("");
  const [analysisBusy, setAnalysisBusy] = useState(false);
  const [analysisRefreshing, setAnalysisRefreshing] = useState(false);

  // Chat state
  const [chatPrompt, setChatPrompt] = useState("");
  const [chatBusy, setChatBusy] = useState(false);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);

  const activePatient = useMemo(
    () => patients.find((patient) => patient.id === activePatientId.trim()) || null,
    [patients, activePatientId],
  );

  const caregiverName = useMemo(() => {
    const joined = `${user.first_name || ""} ${user.last_name || ""}`.trim();
    return joined || user.email.split("@")[0] || "Caregiver";
  }, [user]);

  const publishedPlanCount = useMemo(
    () => taskPlans.filter((p: any) => String(p.status || "").toUpperCase() === "PUBLISHED").length,
    [taskPlans],
  );
  const draftPlanCount = useMemo(
    () => taskPlans.filter((p: any) => String(p.status || "").toUpperCase() === "DRAFT").length,
    [taskPlans],
  );

  const updatePatientForm = (field: keyof PatientFormState, value: string) => {
    setPatientForm((current) => ({ ...current, [field]: value }));
    if (patientStatus) {
      setPatientStatus("");
    }
  };

  const updateSettings = (patch: Partial<DashboardSettings>) => {
    setSettingsForm((current) => ({ ...current, ...patch }));
    if (settingsStatus) {
      setSettingsStatus("");
    }
  };

  const loadTemplates = useCallback(async () => {
    try {
      const res = await apiRequest<{ data: any[] }>(apiBase, "/task-lab/templates", token);
      setTemplates(res.data || []);
      if (res.data.length > 0) {
        setSelectedTemplate((current) => current || res.data[0].key);
      }
    } catch (e) {
      setTaskStatus(e instanceof Error ? e.message : "Failed to load templates.");
    }
  }, [token, apiBase]);

  const loadPatients = useCallback(async () => {
    try {
      const res = await apiRequest<{ data: Patient[] }>(apiBase, "/patients?limit=100&offset=0", token);
      setPatients(res.data || []);
    } catch (e) {
      setPatientStatus(e instanceof Error ? e.message : "Failed to load patients.");
    }
  }, [token, apiBase]);

  const loadTaskPlans = useCallback(
    async (patientId: string) => {
      try {
        const res = await apiRequest<{ data: any[] }>(
          apiBase,
          `/task-lab/plans?patientId=${encodeURIComponent(patientId)}&limit=50&offset=0`,
          token,
        );
        setTaskPlans(res.data || []);
      } catch (e) {
        setTaskStatus(e instanceof Error ? e.message : "Failed to load plans.");
      }
    },
    [token, apiBase],
  );

  const loadOverview = useCallback(async () => {
    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const res = await apiRequest<any>(apiBase, `/analysis/overview?windowMs=${windowMs}`, token);
      setOverview(res);
    } catch (e) {
      setAnalysisStatus(e instanceof Error ? e.message : "Unable to load overview.");
    }
  }, [token, apiBase, analysisWindowHours]);

  const loadPatientSummary = useCallback(async () => {
    if (!activePatientId.trim()) {
      setAnalysisStatus("Set patient ID first.");
      return;
    }
    setAnalysisBusy(true);
    setAnalysisStatus("");
    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const res = await apiRequest<any>(
        apiBase,
        `/analysis/patient/${encodeURIComponent(activePatientId.trim())}/summary?windowMs=${windowMs}`,
        token,
      );
      setSummary(res);
    } catch (e) {
      setAnalysisStatus(e instanceof Error ? e.message : "Unable to load patient summary.");
    } finally {
      setAnalysisBusy(false);
    }
  }, [activePatientId, analysisWindowHours, token, apiBase]);

  const refreshAnalysisBoard = useCallback(async () => {
    setAnalysisRefreshing(true);
    setAnalysisStatus("");
    try {
      await loadOverview();
      if (activePatientId.trim()) {
        await loadPatientSummary();
      }
      setAnalysisStatus("Analysis board refreshed.");
    } catch {
      setAnalysisStatus("Unable to refresh analysis board.");
    } finally {
      setAnalysisRefreshing(false);
    }
  }, [loadOverview, loadPatientSummary, activePatientId]);

  useEffect(() => {
    void loadTemplates();
  }, [loadTemplates]);

  useEffect(() => {
    void loadOverview();
  }, [loadOverview]);

  useEffect(() => {
    void loadPatients();
  }, [loadPatients]);

  useEffect(() => {
    if (activePatientId.trim()) {
      void loadTaskPlans(activePatientId.trim());
    } else {
      setTaskPlans([]);
    }
  }, [activePatientId, loadTaskPlans]);

  useEffect(() => {
    const raw = localStorage.getItem(DASHBOARD_SETTINGS_KEY);
    if (!raw) {
      return;
    }
    try {
      const parsed = JSON.parse(raw) as Partial<DashboardSettings>;
      const loaded: DashboardSettings = {
        defaultPatientId:
          typeof parsed.defaultPatientId === "string" ? parsed.defaultPatientId : "",
        analysisWindowHours:
          typeof parsed.analysisWindowHours === "string"
            ? parsed.analysisWindowHours
            : "24",
        autoRefreshOverview: Boolean(parsed.autoRefreshOverview),
        refreshIntervalMinutes:
          typeof parsed.refreshIntervalMinutes === "string"
            ? parsed.refreshIntervalMinutes
            : "5",
      };
      setSettingsForm(loaded);
      setAnalysisWindowHours(loaded.analysisWindowHours);
      if (loaded.defaultPatientId.trim()) {
        setActivePatientId(loaded.defaultPatientId.trim());
      }
    } catch {
      setSettingsStatus("Saved settings were invalid and were ignored.");
    }
  }, []);

  useEffect(() => {
    if (!settingsForm.autoRefreshOverview) {
      return;
    }
    const minutes = Math.max(
      1,
      parseInt(settingsForm.refreshIntervalMinutes, 10) || 5,
    );
    const timerId = window.setInterval(() => {
      void loadOverview();
    }, minutes * 60_000);
    return () => window.clearInterval(timerId);
  }, [settingsForm.autoRefreshOverview, settingsForm.refreshIntervalMinutes, loadOverview]);

  const createPatient = async () => {
    if (!patientForm.firstName.trim() || !patientForm.lastName.trim()) {
      setPatientStatus("First name and last name are required.");
      return;
    }
    setPatientBusy(true);
    setPatientStatus("");
    try {
      const baselineMs = parseInt(patientForm.baselineResponseTimeMs, 10);
      const created = await apiRequest<Patient>(apiBase, "/patients", token, {
        method: "POST",
        body: {
          first_name: patientForm.firstName.trim(),
          last_name: patientForm.lastName.trim(),
          date_of_birth: patientForm.dateOfBirth || undefined,
          cognitive_condition: patientForm.cognitiveCondition || undefined,
          risk_level: patientForm.riskLevel || "MEDIUM",
          baseline_response_time_ms: Number.isFinite(baselineMs) ? baselineMs : undefined,
        },
      });
      setPatientStatus("Patient added successfully.");
      setActivePatientId(created.id);
      setPatientForm({
        firstName: "",
        lastName: "",
        dateOfBirth: "",
        cognitiveCondition: "",
        riskLevel: "MEDIUM",
        baselineResponseTimeMs: "3000",
      });
      await Promise.all([loadPatients(), loadOverview()]);
    } catch (e) {
      setPatientStatus(e instanceof Error ? e.message : "Failed to add patient.");
    } finally {
      setPatientBusy(false);
    }
  };

  const saveSettings = async () => {
    const normalized: DashboardSettings = {
      defaultPatientId: settingsForm.defaultPatientId.trim(),
      analysisWindowHours: String(
        Math.max(1, parseInt(settingsForm.analysisWindowHours, 10) || 24),
      ),
      autoRefreshOverview: settingsForm.autoRefreshOverview,
      refreshIntervalMinutes: String(
        Math.max(1, parseInt(settingsForm.refreshIntervalMinutes, 10) || 5),
      ),
    };

    localStorage.setItem(DASHBOARD_SETTINGS_KEY, JSON.stringify(normalized));
    setSettingsForm(normalized);
    setAnalysisWindowHours(normalized.analysisWindowHours);
    if (normalized.defaultPatientId) {
      setActivePatientId(normalized.defaultPatientId);
    }
    setSettingsStatus("Settings saved.");
    await loadOverview();
  };

  const generateTaskDraft = async () => {
    if (!taskPrompt.trim()) {
      setTaskStatus("Enter a prompt first.");
      return;
    }
    setTaskBusy(true);
    setTaskStatus("");
    try {
      const res = await apiRequest<any>(apiBase, "/ai/task-lab/generate", token, {
        method: "POST",
        body: {
          prompt: taskPrompt,
          patientProfile: activePatient
            ? {
                id: activePatient.id,
                first_name: activePatient.first_name,
                last_name: activePatient.last_name,
                cognitive_condition: activePatient.cognitive_condition,
                risk_level: activePatient.risk_level,
              }
            : undefined,
          constraints: activePatient?.risk_level
            ? {
                riskLevel: activePatient.risk_level,
              }
            : undefined,
        },
      });
      setGeneratedDraft(res);
      setTaskStatus("AI draft generated. Review and publish to Task Lab.");
    } catch (e) {
      setTaskStatus(e instanceof Error ? e.message : "Draft generation failed.");
    } finally {
      setTaskBusy(false);
    }
  };

  const createTemplatePlan = async () => {
    if (!activePatientId.trim()) {
      setTaskStatus("Set patient ID first.");
      return;
    }
    if (!selectedTemplate) {
      setTaskStatus("Select a template first.");
      return;
    }
    setTaskBusy(true);
    setTaskStatus("");
    try {
      await apiRequest(apiBase, "/task-lab/plans/from-template", token, {
        method: "POST",
        body: {
          patient_id: activePatientId.trim(),
          template_key: selectedTemplate,
        },
      });
      setTaskStatus("Template plan created.");
      await loadTaskPlans(activePatientId.trim());
    } catch (e) {
      setTaskStatus(e instanceof Error ? e.message : "Template creation failed.");
    } finally {
      setTaskBusy(false);
    }
  };

  const publishGeneratedDraft = async () => {
    if (!generatedDraft || !activePatientId.trim()) {
      setTaskStatus("Set patient ID before publishing.");
      return;
    }
    setTaskBusy(true);
    setTaskStatus("");
    try {
      await apiRequest(apiBase, "/task-lab/plans", token, {
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
          steps: (generatedDraft.draft.steps || []).map((s: any, i: number) => ({
            step_order: s.step_order || i + 1,
            title: s.title,
            details: s.details,
            is_required: s.is_required !== false,
          })),
        },
      });
      setTaskStatus("Draft saved to Task Lab.");
      await loadTaskPlans(activePatientId.trim());
    } catch (e) {
      setTaskStatus(e instanceof Error ? e.message : "Unable to publish draft.");
    } finally {
      setTaskBusy(false);
    }
  };

  const publishPlan = async (planId: string) => {
    setTaskBusy(true);
    setTaskStatus("");
    try {
      await apiRequest(apiBase, `/task-lab/plans/${planId}/status`, token, {
        method: "PUT",
        body: { status: "PUBLISHED" },
      });
      setTaskStatus("Plan published.");
      if (activePatientId.trim()) {
        await loadTaskPlans(activePatientId.trim());
      }
    } catch (e) {
      setTaskStatus(
        e instanceof Error ? e.message : "Failed to update plan status.",
      );
    } finally {
      setTaskBusy(false);
    }
  };

  const sendChat = async () => {
    const prompt = chatPrompt.trim();
    if (!prompt) {
      return;
    }

    const userMessage: ChatMessage = {
      id: `chat-user-${Date.now()}`,
      role: "user",
      text: prompt,
      timestamp: Date.now(),
    };

    setChatMessages((prev) => [...prev, userMessage]);
    setChatPrompt("");
    setChatBusy(true);

    try {
      const conversationHistory = chatMessages
        .filter((message) => message.role !== "system")
        .map((message) => ({
          role: message.role,
          content: message.text,
        }));

      const payload = await apiRequest<any>(apiBase, "/ai/chat", token, {
        method: "POST",
        body: {
          prompt,
          patientId: activePatientId.trim() || undefined,
          roleContext: user.role || "CAREGIVER",
          conversationHistory,
        },
      });

      const actionItems = Array.isArray(payload.actionItems)
        ? payload.actionItems
        : [];
      const safetyFlags = Array.isArray(payload.safetyFlags)
        ? payload.safetyFlags
        : [];

      setChatMessages((prev) => [
        ...prev,
        {
          id: `chat-assistant-${Date.now()}`,
          role: "assistant",
          text: typeof payload.reply === "string" ? payload.reply : "No response text received.",
          actionItems,
          safetyFlags,
          confidence:
            typeof payload.confidence === "number" && Number.isFinite(payload.confidence)
              ? Math.max(0, Math.min(1, payload.confidence))
              : undefined,
          timestamp: Date.now(),
        },
      ]);
    } catch (e) {
      setChatMessages((prev) => [
        ...prev,
        {
          id: `chat-system-${Date.now()}`,
          role: "system",
          text: e instanceof Error ? e.message : "Unable to contact AI endpoint.",
          timestamp: Date.now(),
        },
      ]);
    } finally {
      setChatBusy(false);
    }
  };

  const tabs = [
    { key: "patients" as const, label: "Patients" },
    { key: "tasklab" as const, label: "Task Lab" },
    { key: "analysis" as const, label: "Analysis Board" },
    { key: "chat" as const, label: "AI Chat" },
    { key: "settings" as const, label: "Settings" },
  ];

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto p-4 md:p-6 space-y-6">
        <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}>
          <DashboardHeader
            caregiverName={caregiverName}
            onLogout={onLogout}
            onOpenSettings={() => setActiveTab("settings")}
          />
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
        >
          <KpiRow
            patients={overview?.totals?.patients ?? patients.length}
            unacknowledgedAlerts={overview?.totals?.unacknowledgedAlerts ?? null}
            publishedPlans={publishedPlanCount}
            draftPlans={draftPlanCount}
          />
        </motion.div>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
          className="grid sm:grid-cols-2 gap-4"
        >
          <div className="space-y-2">
            <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Active Patient ID
            </Label>
            <Input
              value={activePatientId}
              onChange={(e) => setActivePatientId(e.target.value)}
              placeholder="Paste patient UUID"
            />
          </div>
          <div className="space-y-2">
            <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Analysis Window (hours)
            </Label>
            <Input
              type="number"
              value={analysisWindowHours}
              onChange={(e) => setAnalysisWindowHours(e.target.value)}
              placeholder="24"
            />
          </div>
        </motion.div>

        <div className="flex gap-2 flex-wrap">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-5 py-2.5 rounded-xl text-sm font-semibold transition-all duration-200 border ${
                activeTab === tab.key
                  ? "gradient-primary-bg text-primary-foreground border-transparent shadow-md"
                  : "bg-card text-muted-foreground border-border hover:bg-secondary"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
        >
          {activeTab === "patients" && (
            <PatientsPanel
              patients={patients}
              activePatientId={activePatientId}
              patientForm={patientForm}
              patientBusy={patientBusy}
              patientStatus={patientStatus}
              onSelectPatient={setActivePatientId}
              onChangeForm={updatePatientForm}
              onCreatePatient={createPatient}
              onRefreshPatients={() => void loadPatients()}
            />
          )}

          {activeTab === "tasklab" && (
            <TaskLabPanel
              templates={templates}
              selectedTemplate={selectedTemplate}
              onSelectTemplate={setSelectedTemplate}
              onCreateTemplate={createTemplatePlan}
              taskPrompt={taskPrompt}
              onSetTaskPrompt={setTaskPrompt}
              onGenerateDraft={generateTaskDraft}
              generatedDraft={generatedDraft}
              onPublishDraft={publishGeneratedDraft}
              taskPlans={taskPlans}
              onPublishPlan={publishPlan}
              taskBusy={taskBusy}
              taskStatus={taskStatus}
            />
          )}

          {activeTab === "analysis" && (
            <AnalysisPanel
              overview={overview}
              summary={summary}
              onLoadSummary={loadPatientSummary}
              onRefreshBoard={() => void refreshAnalysisBoard()}
              analysisRefreshing={analysisRefreshing}
              analysisBusy={analysisBusy}
              analysisStatus={analysisStatus}
              hasPatientId={!!activePatientId.trim()}
            />
          )}

          {activeTab === "chat" && (
            <ChatPanel
              messages={chatMessages}
              prompt={chatPrompt}
              onSetPrompt={setChatPrompt}
              onSend={sendChat}
              busy={chatBusy}
            />
          )}

          {activeTab === "settings" && (
            <SettingsPanel
              userEmail={user.email}
              settings={settingsForm}
              settingsStatus={settingsStatus}
              onSettingsChange={updateSettings}
              onSaveSettings={() => void saveSettings()}
            />
          )}
        </motion.div>
      </div>
    </div>
  );
};

export default DashboardView;
