/* eslint-disable @typescript-eslint/no-explicit-any */

import { useCallback, useEffect, useMemo, useState } from "react";
import { motion } from "framer-motion";
import DashboardHeader from "./DashboardHeader";
import KpiRow from "./KpiRow";
import TaskLabPanel from "./TaskLabPanel";
import AnalysisPanel from "./AnalysisPanel";
import ChatPanel from "./ChatPanel";
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

const DashboardView = ({ token, user, apiBase, onLogout }: DashboardViewProps) => {
  const [activeTab, setActiveTab] = useState<"tasklab" | "analysis" | "chat">("tasklab");
  const [activePatientId, setActivePatientId] = useState("");
  const [analysisWindowHours, setAnalysisWindowHours] = useState("24");

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

  // Chat state
  const [chatPrompt, setChatPrompt] = useState("");
  const [chatBusy, setChatBusy] = useState(false);
  const [chatMessages, setChatMessages] = useState<string[]>([]);

  const caregiverName = useMemo(() => {
    const joined = `${user.first_name || ""} ${user.last_name || ""}`.trim();
    return joined || user.email.split("@")[0] || "Caregiver";
  }, [user]);

  const publishedPlanCount = useMemo(() => taskPlans.filter((p: any) => String(p.status || "").toUpperCase() === "PUBLISHED").length, [taskPlans]);
  const draftPlanCount = useMemo(() => taskPlans.filter((p: any) => String(p.status || "").toUpperCase() === "DRAFT").length, [taskPlans]);

  const loadTemplates = useCallback(async () => {
    try {
      const res = await apiRequest<{ data: any[] }>(apiBase, "/task-lab/templates", token);
      setTemplates(res.data || []);
      if (res.data.length > 0) setSelectedTemplate((c) => c || res.data[0].key);
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Failed to load templates."); }
  }, [token, apiBase]);

  const loadTaskPlans = useCallback(async (patientId: string) => {
    try {
      const res = await apiRequest<{ data: any[] }>(apiBase, `/task-lab/plans?patientId=${encodeURIComponent(patientId)}&limit=50&offset=0`, token);
      setTaskPlans(res.data || []);
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Failed to load plans."); }
  }, [token, apiBase]);

  const loadOverview = useCallback(async () => {
    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const res = await apiRequest<any>(apiBase, `/analysis/overview?windowMs=${windowMs}`, token);
      setOverview(res);
    } catch (e) { setAnalysisStatus(e instanceof Error ? e.message : "Unable to load overview."); }
  }, [token, apiBase, analysisWindowHours]);

  useEffect(() => { void loadTemplates(); }, [loadTemplates]);
  useEffect(() => { void loadOverview(); }, [loadOverview]);
  useEffect(() => {
    if (activePatientId.trim()) void loadTaskPlans(activePatientId.trim());
    else setTaskPlans([]);
  }, [activePatientId, loadTaskPlans]);

  const generateTaskDraft = async () => {
    if (!taskPrompt.trim()) { setTaskStatus("Enter a prompt first."); return; }
    setTaskBusy(true); setTaskStatus("");
    try {
      const res = await apiRequest<any>(apiBase, "/ai/task-lab/generate", token, { method: "POST", body: { prompt: taskPrompt } });
      setGeneratedDraft(res);
      setTaskStatus("AI draft generated. Review and publish to Task Lab.");
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Draft generation failed."); }
    finally { setTaskBusy(false); }
  };

  const createTemplatePlan = async () => {
    if (!activePatientId.trim()) { setTaskStatus("Set patient ID first."); return; }
    if (!selectedTemplate) { setTaskStatus("Select a template first."); return; }
    setTaskBusy(true); setTaskStatus("");
    try {
      await apiRequest(apiBase, "/task-lab/plans/from-template", token, { method: "POST", body: { patient_id: activePatientId.trim(), template_key: selectedTemplate } });
      setTaskStatus("Template plan created.");
      await loadTaskPlans(activePatientId.trim());
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Template creation failed."); }
    finally { setTaskBusy(false); }
  };

  const publishGeneratedDraft = async () => {
    if (!generatedDraft || !activePatientId.trim()) { setTaskStatus("Set patient ID before publishing."); return; }
    setTaskBusy(true); setTaskStatus("");
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
            step_order: s.step_order || i + 1, title: s.title, details: s.details, is_required: s.is_required !== false,
          })),
        },
      });
      setTaskStatus("Draft saved to Task Lab.");
      await loadTaskPlans(activePatientId.trim());
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Unable to publish draft."); }
    finally { setTaskBusy(false); }
  };

  const publishPlan = async (planId: string) => {
    setTaskBusy(true); setTaskStatus("");
    try {
      await apiRequest(apiBase, `/task-lab/plans/${planId}/status`, token, { method: "PUT", body: { status: "PUBLISHED" } });
      setTaskStatus("Plan published.");
      if (activePatientId.trim()) await loadTaskPlans(activePatientId.trim());
    } catch (e) { setTaskStatus(e instanceof Error ? e.message : "Failed to update plan status."); }
    finally { setTaskBusy(false); }
  };

  const loadPatientSummary = async () => {
    if (!activePatientId.trim()) { setAnalysisStatus("Set patient ID first."); return; }
    setAnalysisBusy(true); setAnalysisStatus("");
    try {
      const windowMs = Math.max(1, parseInt(analysisWindowHours, 10) || 24) * 3_600_000;
      const res = await apiRequest<any>(apiBase, `/analysis/patient/${encodeURIComponent(activePatientId.trim())}/summary?windowMs=${windowMs}`, token);
      setSummary(res);
    } catch (e) { setAnalysisStatus(e instanceof Error ? e.message : "Unable to load patient summary."); }
    finally { setAnalysisBusy(false); }
  };

  const sendChat = async () => {
    if (!chatPrompt.trim()) return;
    setChatBusy(true);
    try {
      const payload = await apiRequest<any>(apiBase, "/ai/chat", token, {
        method: "POST",
        body: { prompt: chatPrompt, patientId: activePatientId.trim() || undefined },
      });
      const actionLines = payload.actionItems.map((item: string) => `- ${item}`).join("\n");
      const flags = payload.safetyFlags.length > 0 ? payload.safetyFlags.join(", ") : "None";
      setChatMessages((prev) => [
        ...prev,
        `You: ${chatPrompt}`,
        `Assistant: ${payload.reply}\nActions:\n${actionLines}\nSafety Flags: ${flags}`,
      ]);
      setChatPrompt("");
    } catch (e) {
      setChatMessages((prev) => [...prev, `System: ${e instanceof Error ? e.message : "Unable to contact AI endpoint."}`]);
    } finally { setChatBusy(false); }
  };

  const tabs = [
    { key: "tasklab" as const, label: "Task Lab" },
    { key: "analysis" as const, label: "Analysis" },
    { key: "chat" as const, label: "AI Chat" },
  ];

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-7xl mx-auto p-4 md:p-6 space-y-6">
        <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}>
          <DashboardHeader caregiverName={caregiverName} onLogout={onLogout} />
        </motion.div>

        <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
          <KpiRow
            patients={overview?.totals?.patients ?? null}
            unacknowledgedAlerts={overview?.totals?.unacknowledgedAlerts ?? null}
            publishedPlans={publishedPlanCount}
            draftPlans={draftPlanCount}
          />
        </motion.div>

        {/* Filters */}
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.2 }} className="grid sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Active Patient ID</Label>
            <Input value={activePatientId} onChange={(e) => setActivePatientId(e.target.value)} placeholder="Paste patient UUID" />
          </div>
          <div className="space-y-2">
            <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Analysis Window (hours)</Label>
            <Input type="number" value={analysisWindowHours} onChange={(e) => setAnalysisWindowHours(e.target.value)} placeholder="24" />
          </div>
        </motion.div>

        {/* Tabs */}
        <div className="flex gap-2">
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

        {/* Tab content */}
        <motion.div key={activeTab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
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
        </motion.div>
      </div>
    </div>
  );
};

export default DashboardView;
