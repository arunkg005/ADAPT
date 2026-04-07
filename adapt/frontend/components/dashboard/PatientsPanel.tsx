import { PlusCircle, RefreshCcw, UserRoundCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

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

interface PatientsPanelProps {
  patients: Patient[];
  activePatientId: string;
  patientForm: PatientFormState;
  patientBusy: boolean;
  patientStatus: string;
  onSelectPatient: (patientId: string) => void;
  onChangeForm: (field: keyof PatientFormState, value: string) => void;
  onCreatePatient: () => void;
  onRefreshPatients: () => void;
}

const PatientsPanel = ({
  patients,
  activePatientId,
  patientForm,
  patientBusy,
  patientStatus,
  onSelectPatient,
  onChangeForm,
  onCreatePatient,
  onRefreshPatients,
}: PatientsPanelProps) => {
  return (
    <div className="grid lg:grid-cols-3 gap-4">
      <div className="glass-panel p-6 space-y-4 lg:col-span-2">
        <div className="flex items-center gap-2">
          <PlusCircle className="h-5 w-5 text-accent" />
          <h3 className="text-base font-heading font-bold text-foreground">Add Patient</h3>
        </div>
        <p className="text-xs text-muted-foreground">Create a new patient profile and make it active for Task Lab and Analysis.</p>

        <div className="grid sm:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="patient-first-name">First Name</Label>
            <Input
              id="patient-first-name"
              value={patientForm.firstName}
              onChange={(e) => onChangeForm("firstName", e.target.value)}
              placeholder="John"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="patient-last-name">Last Name</Label>
            <Input
              id="patient-last-name"
              value={patientForm.lastName}
              onChange={(e) => onChangeForm("lastName", e.target.value)}
              placeholder="Doe"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="patient-dob">Date of Birth</Label>
            <Input
              id="patient-dob"
              type="date"
              value={patientForm.dateOfBirth}
              onChange={(e) => onChangeForm("dateOfBirth", e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="patient-condition">Cognitive Condition</Label>
            <Input
              id="patient-condition"
              value={patientForm.cognitiveCondition}
              onChange={(e) => onChangeForm("cognitiveCondition", e.target.value)}
              placeholder="Mild cognitive impairment"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="patient-risk-level">Risk Level</Label>
            <select
              id="patient-risk-level"
              value={patientForm.riskLevel}
              onChange={(e) => onChangeForm("riskLevel", e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="patient-baseline">Baseline Response (ms)</Label>
            <Input
              id="patient-baseline"
              type="number"
              value={patientForm.baselineResponseTimeMs}
              onChange={(e) => onChangeForm("baselineResponseTimeMs", e.target.value)}
              placeholder="3000"
            />
          </div>
        </div>

        <Button onClick={onCreatePatient} disabled={patientBusy} className="w-full sm:w-auto">
          {patientBusy ? "Saving..." : "Add Patient"}
        </Button>

        {patientStatus && (
          <p
            className={`text-sm font-medium ${
              patientStatus.toLowerCase().includes("failed") ||
              patientStatus.toLowerCase().includes("error")
                ? "text-destructive"
                : "text-success"
            }`}
          >
            {patientStatus}
          </p>
        )}
      </div>

      <div className="glass-panel p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-heading font-bold text-foreground">Patient List</h3>
          <Button size="sm" variant="outline" onClick={onRefreshPatients} disabled={patientBusy}>
            <RefreshCcw className="h-3.5 w-3.5 mr-1" /> Refresh
          </Button>
        </div>

        {patients.length === 0 ? (
          <p className="text-xs text-muted-foreground">No patients yet. Add one to start using the dashboard workflows.</p>
        ) : (
          <div className="space-y-3 max-h-96 overflow-auto">
            {patients.map((patient) => {
              const fullName = `${patient.first_name} ${patient.last_name}`.trim();
              const selected = patient.id === activePatientId;
              return (
                <div
                  key={patient.id}
                  className={`rounded-xl border p-3 space-y-2 ${
                    selected ? "border-primary bg-primary/5" : "border-border bg-background"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-foreground truncate">{fullName || "Unnamed patient"}</p>
                      <p className="text-xs text-muted-foreground truncate">{patient.id}</p>
                    </div>
                    <span className="px-2 py-1 rounded-md bg-secondary text-secondary-foreground text-xs font-medium whitespace-nowrap">
                      {patient.risk_level || "MEDIUM"}
                    </span>
                  </div>
                  {patient.cognitive_condition && (
                    <p className="text-xs text-muted-foreground">{patient.cognitive_condition}</p>
                  )}
                  <Button
                    size="sm"
                    variant={selected ? "secondary" : "outline"}
                    onClick={() => onSelectPatient(patient.id)}
                    className="w-full"
                  >
                    <UserRoundCheck className="h-3.5 w-3.5 mr-1" />
                    {selected ? "Active Patient" : "Set Active"}
                  </Button>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientsPanel;