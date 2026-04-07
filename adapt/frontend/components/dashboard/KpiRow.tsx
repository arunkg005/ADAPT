import { Users, AlertTriangle, CheckCircle, FileText } from "lucide-react";

interface KpiRowProps {
  patients: number | null;
  unacknowledgedAlerts: number | null;
  publishedPlans: number;
  draftPlans: number;
}

const kpis = [
  { key: "patients", label: "Patients", icon: Users, color: "text-primary" },
  { key: "alerts", label: "Unacknowledged Alerts", icon: AlertTriangle, color: "text-destructive" },
  { key: "published", label: "Published Plans", icon: CheckCircle, color: "text-success" },
  { key: "drafts", label: "Draft Plans", icon: FileText, color: "text-accent" },
] as const;

const KpiRow = ({ patients, unacknowledgedAlerts, publishedPlans, draftPlans }: KpiRowProps) => {
  const values: Record<string, number | string> = {
    patients: patients ?? "--",
    alerts: unacknowledgedAlerts ?? "--",
    published: publishedPlans,
    drafts: draftPlans,
  };

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {kpis.map(({ key, label, icon: Icon, color }) => (
        <div key={key} className="glass-panel p-5 space-y-3">
          <div className="flex items-center justify-between">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
            <Icon className={`h-5 w-5 ${color}`} />
          </div>
          <h3 className="text-2xl font-heading font-bold text-foreground">{values[key]}</h3>
        </div>
      ))}
    </div>
  );
};

export default KpiRow;
