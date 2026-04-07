import { LogOut, Settings } from "lucide-react";
import { Button } from "@/components/ui/button";

interface DashboardHeaderProps {
  caregiverName: string;
  onLogout: () => void;
  onOpenSettings: () => void;
}

const DashboardHeader = ({ caregiverName, onLogout, onOpenSettings }: DashboardHeaderProps) => {
  return (
    <header className="glass-panel px-6 py-4 flex items-center justify-between">
      <div className="flex items-center gap-4">
        <div className="w-10 h-10 rounded-xl gradient-accent-bg flex items-center justify-center">
          <span className="text-accent-foreground font-bold text-lg font-heading">A</span>
        </div>
        <div>
          <h1 className="text-xl md:text-2xl font-heading font-bold text-foreground">{caregiverName}</h1>
          <p className="text-xs text-muted-foreground">Caregiver Console</p>
        </div>
      </div>
      <div className="flex gap-2">
        <Button variant="ghost" size="icon" onClick={onOpenSettings}>
          <Settings className="h-5 w-5" />
        </Button>
        <Button variant="ghost" size="sm" onClick={onLogout} className="text-destructive hover:text-destructive">
          <LogOut className="h-4 w-4 mr-2" /> Sign Out
        </Button>
      </div>
    </header>
  );
};

export default DashboardHeader;
