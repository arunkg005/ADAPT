import { BarChart3, TrendingUp, AlertTriangle, Activity } from "lucide-react";
import { Button } from "@/components/ui/button";

interface OverviewData {
  totals: {
    patients: number;
    alerts: number;
    unacknowledgedAlerts: number;
    telemetrySignals: number;
  };
  taskLab: { byStatus: Record<string, number> };
}

interface PatientSummary {
  patient: { id: string; name: string; riskLevel: string };
  telemetry: { total: number; byType: Array<{ signalType: string; count: number }> };
  alerts: { total: number; unacknowledged: number; bySeverity: Record<string, number> };
  taskLab: { byStatus: Record<string, number> };
  insight: { dominantIssue: string; trend: string; recommendation: string };
}

interface AnalysisPanelProps {
  overview: OverviewData | null;
  summary: PatientSummary | null;
  onLoadSummary: () => void;
  analysisBusy: boolean;
  analysisStatus: string;
  hasPatientId: boolean;
}

const AnalysisPanel = ({ overview, summary, onLoadSummary, analysisBusy, analysisStatus, hasPatientId }: AnalysisPanelProps) => {
  return (
    <div className="space-y-6">
      {/* Overview */}
      {overview && (
        <div className="glass-panel p-6 space-y-4">
          <div className="flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-accent" />
            <h3 className="text-base font-heading font-bold text-foreground">Platform Overview</h3>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: "Total Patients", value: overview.totals.patients, icon: Activity },
              { label: "Total Alerts", value: overview.totals.alerts, icon: AlertTriangle },
              { label: "Unacknowledged", value: overview.totals.unacknowledgedAlerts, icon: AlertTriangle },
              { label: "Telemetry Signals", value: overview.totals.telemetrySignals, icon: TrendingUp },
            ].map(({ label, value, icon: Icon }) => (
              <div key={label} className="rounded-xl border border-border bg-background p-4 space-y-1">
                <p className="text-xs text-muted-foreground">{label}</p>
                <div className="flex items-center gap-2">
                  <Icon className="h-4 w-4 text-accent" />
                  <span className="text-xl font-heading font-bold text-foreground">{value}</span>
                </div>
              </div>
            ))}
          </div>
          {overview.taskLab.byStatus && Object.keys(overview.taskLab.byStatus).length > 0 && (
            <div className="flex flex-wrap gap-2">
              {Object.entries(overview.taskLab.byStatus).map(([status, count]) => (
                <span key={status} className="px-3 py-1 rounded-full bg-secondary text-secondary-foreground text-xs font-medium">
                  {status}: {String(count)}
                </span>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Patient Summary */}
      <div className="glass-panel p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-heading font-bold text-foreground">Patient Summary</h3>
          <Button size="sm" onClick={onLoadSummary} disabled={analysisBusy || !hasPatientId}>
            {analysisBusy ? "Loading..." : "Load Summary"}
          </Button>
        </div>

        {!hasPatientId && <p className="text-xs text-muted-foreground">Set a patient ID above to load summary.</p>}

        {analysisStatus && (
          <p className="text-sm text-destructive font-medium">{analysisStatus}</p>
        )}

        {summary && (
          <div className="space-y-4">
            <div className="flex items-center gap-3 pb-3 border-b border-border">
              <div className="w-10 h-10 rounded-xl gradient-primary-bg flex items-center justify-center">
                <span className="text-primary-foreground font-bold font-heading">
                  {summary.patient.name.charAt(0)}
                </span>
              </div>
              <div>
                <p className="font-semibold text-foreground">{summary.patient.name}</p>
                <p className="text-xs text-muted-foreground">Risk: {summary.patient.riskLevel}</p>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="rounded-xl border border-border bg-background p-4">
                <p className="text-xs text-muted-foreground mb-1">Telemetry</p>
                <p className="text-lg font-heading font-bold text-foreground">{summary.telemetry.total}</p>
              </div>
              <div className="rounded-xl border border-border bg-background p-4">
                <p className="text-xs text-muted-foreground mb-1">Alerts</p>
                <p className="text-lg font-heading font-bold text-foreground">{summary.alerts.total}</p>
                <p className="text-xs text-muted-foreground">{summary.alerts.unacknowledged} unacknowledged</p>
              </div>
            </div>

            {summary.insight && (
              <div className="rounded-xl border border-accent/20 bg-accent/5 p-4 space-y-2">
                <p className="text-xs font-bold uppercase tracking-widest text-accent">AI Insight</p>
                <p className="text-sm text-foreground"><strong>Issue:</strong> {summary.insight.dominantIssue}</p>
                <p className="text-sm text-foreground"><strong>Trend:</strong> {summary.insight.trend}</p>
                <p className="text-sm text-foreground"><strong>Recommendation:</strong> {summary.insight.recommendation}</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AnalysisPanel;
