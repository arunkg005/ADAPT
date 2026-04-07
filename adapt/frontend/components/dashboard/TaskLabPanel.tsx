import { Sparkles, FileText, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface TaskTemplate {
  key: string;
  name: string;
  risk_level: string;
}

interface TaskPlan {
  id: string;
  title: string;
  status: string;
  task_type: string;
  risk_level: string;
  source: string;
  created_at: string;
}

interface TaskDraft {
  message: string;
  guidance: string;
  draft: {
    title: string;
    description?: string;
    steps?: Array<{ step_order: number; title: string; details?: string; is_required: boolean }>;
  };
}

interface TaskLabPanelProps {
  templates: TaskTemplate[];
  selectedTemplate: string;
  onSelectTemplate: (key: string) => void;
  onCreateTemplate: () => void;
  taskPrompt: string;
  onSetTaskPrompt: (val: string) => void;
  onGenerateDraft: () => void;
  generatedDraft: TaskDraft | null;
  onPublishDraft: () => void;
  taskPlans: TaskPlan[];
  onPublishPlan: (id: string) => void;
  taskBusy: boolean;
  taskStatus: string;
}

const TaskLabPanel = ({
  templates, selectedTemplate, onSelectTemplate, onCreateTemplate,
  taskPrompt, onSetTaskPrompt, onGenerateDraft,
  generatedDraft, onPublishDraft,
  taskPlans, onPublishPlan,
  taskBusy, taskStatus,
}: TaskLabPanelProps) => {
  return (
    <div className="grid lg:grid-cols-3 gap-4">
      {/* Templates */}
      <div className="glass-panel p-6 space-y-4">
        <div className="flex items-center gap-2 mb-2">
          <FileText className="h-5 w-5 text-accent" />
          <h3 className="text-base font-heading font-bold text-foreground">Template Plans</h3>
        </div>
        <p className="text-xs text-muted-foreground">Create plans quickly from predefined templates.</p>
        <select
          value={selectedTemplate}
          onChange={(e) => onSelectTemplate(e.target.value)}
          className="w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
        >
          {templates.map((t) => (
            <option key={t.key} value={t.key}>{t.name} ({t.risk_level})</option>
          ))}
        </select>
        <Button variant="default" size="sm" className="w-full" onClick={onCreateTemplate} disabled={taskBusy}>
          Use Template
        </Button>
      </div>

      {/* AI Draft */}
      <div className="glass-panel p-6 space-y-4">
        <div className="flex items-center gap-2 mb-2">
          <Sparkles className="h-5 w-5 text-accent" />
          <h3 className="text-base font-heading font-bold text-foreground">AI Draft Generator</h3>
        </div>
        <p className="text-xs text-muted-foreground">Describe what you need and let AI create a draft plan.</p>
        <Input
          value={taskPrompt}
          onChange={(e) => onSetTaskPrompt(e.target.value)}
          placeholder="e.g. Morning medication routine for elderly patient"
        />
        <Button variant="accent" size="sm" className="w-full" onClick={onGenerateDraft} disabled={taskBusy}>
          <Sparkles className="h-4 w-4 mr-1" /> Generate Draft
        </Button>
        {generatedDraft && (
          <div className="rounded-xl border border-border bg-secondary/50 p-4 space-y-3">
            <h4 className="text-sm font-bold text-foreground">{generatedDraft.draft.title}</h4>
            {generatedDraft.draft.description && (
              <p className="text-xs text-muted-foreground">{generatedDraft.draft.description}</p>
            )}
            {generatedDraft.draft.steps && generatedDraft.draft.steps.length > 0 && (
              <ol className="list-decimal list-inside text-xs text-muted-foreground space-y-1">
                {generatedDraft.draft.steps.map((s, i) => (
                  <li key={i}>{s.title}</li>
                ))}
              </ol>
            )}
            <p className="text-xs text-muted-foreground italic">{generatedDraft.guidance}</p>
            <Button size="sm" className="w-full" onClick={onPublishDraft} disabled={taskBusy}>
              <Send className="h-3.5 w-3.5 mr-1" /> Publish to Task Lab
            </Button>
          </div>
        )}
      </div>

      {/* Plans List */}
      <div className="glass-panel p-6 space-y-4 lg:col-span-1">
        <h3 className="text-base font-heading font-bold text-foreground">Active Plans</h3>
        {taskPlans.length === 0 ? (
          <p className="text-xs text-muted-foreground">No plans for this patient yet.</p>
        ) : (
          <div className="space-y-3 max-h-80 overflow-auto">
            {taskPlans.map((plan) => (
              <div key={plan.id} className="rounded-xl border border-border bg-background p-3 flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-foreground truncate">{plan.title}</p>
                  <p className="text-xs text-muted-foreground mt-1">
                    {plan.task_type} · {plan.risk_level} · {plan.source}
                  </p>
                </div>
                {String(plan.status).toUpperCase() === "DRAFT" ? (
                  <Button size="sm" variant="outline" onClick={() => onPublishPlan(plan.id)} disabled={taskBusy}>
                    Publish
                  </Button>
                ) : (
                  <span className="px-2 py-1 rounded-md bg-success/10 text-success text-xs font-semibold whitespace-nowrap">
                    Published
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {taskStatus && (
        <p className={`lg:col-span-3 text-sm font-medium ${taskStatus.toLowerCase().includes("fail") || taskStatus.toLowerCase().includes("error") ? "text-destructive" : "text-success"}`}>
          {taskStatus}
        </p>
      )}
    </div>
  );
};

export default TaskLabPanel;
