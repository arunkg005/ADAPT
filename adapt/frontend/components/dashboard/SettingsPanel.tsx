import { Save, Settings2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface DashboardSettings {
  defaultPatientId: string;
  analysisWindowHours: string;
  autoRefreshOverview: boolean;
  refreshIntervalMinutes: string;
}

interface SettingsPanelProps {
  userEmail: string;
  settings: DashboardSettings;
  settingsStatus: string;
  onSettingsChange: (patch: Partial<DashboardSettings>) => void;
  onSaveSettings: () => void;
}

const SettingsPanel = ({
  userEmail,
  settings,
  settingsStatus,
  onSettingsChange,
  onSaveSettings,
}: SettingsPanelProps) => {
  return (
    <div className="grid lg:grid-cols-3 gap-4">
      <div className="glass-panel p-6 space-y-3">
        <div className="flex items-center gap-2">
          <Settings2 className="h-5 w-5 text-accent" />
          <h3 className="text-base font-heading font-bold text-foreground">Profile</h3>
        </div>
        <p className="text-xs text-muted-foreground">Signed in account</p>
        <p className="text-sm font-semibold text-foreground break-all">{userEmail}</p>
        <p className="text-xs text-muted-foreground">
          These settings are stored in your browser for this dashboard.
        </p>
      </div>

      <div className="glass-panel p-6 space-y-4 lg:col-span-2">
        <h3 className="text-base font-heading font-bold text-foreground">Dashboard Settings</h3>

        <div className="grid sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="settings-default-patient">Default Patient ID</Label>
            <Input
              id="settings-default-patient"
              value={settings.defaultPatientId}
              onChange={(e) => onSettingsChange({ defaultPatientId: e.target.value })}
              placeholder="UUID of patient to auto-select"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="settings-analysis-window">Default Analysis Window (hours)</Label>
            <Input
              id="settings-analysis-window"
              type="number"
              min="1"
              value={settings.analysisWindowHours}
              onChange={(e) => onSettingsChange({ analysisWindowHours: e.target.value })}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="settings-refresh-interval">Auto-Refresh Interval (minutes)</Label>
            <Input
              id="settings-refresh-interval"
              type="number"
              min="1"
              value={settings.refreshIntervalMinutes}
              onChange={(e) => onSettingsChange({ refreshIntervalMinutes: e.target.value })}
              disabled={!settings.autoRefreshOverview}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="settings-auto-refresh">Analysis Auto-Refresh</Label>
            <label
              htmlFor="settings-auto-refresh"
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm flex items-center gap-2 cursor-pointer"
            >
              <input
                id="settings-auto-refresh"
                type="checkbox"
                checked={settings.autoRefreshOverview}
                onChange={(e) => onSettingsChange({ autoRefreshOverview: e.target.checked })}
              />
              <span className="text-muted-foreground">Enable automatic analysis board refresh</span>
            </label>
          </div>
        </div>

        <Button onClick={onSaveSettings} className="w-full sm:w-auto">
          <Save className="h-4 w-4 mr-2" /> Save Settings
        </Button>

        {settingsStatus && (
          <p
            className={`text-sm font-medium ${
              settingsStatus.toLowerCase().includes("invalid") ||
              settingsStatus.toLowerCase().includes("error")
                ? "text-destructive"
                : "text-success"
            }`}
          >
            {settingsStatus}
          </p>
        )}
      </div>
    </div>
  );
};

export default SettingsPanel;