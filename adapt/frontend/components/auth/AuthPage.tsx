import { FormEvent, useState } from "react";
import { motion } from "framer-motion";
import { ArrowLeft, Shield, Lock, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

interface AuthPageProps {
  onBack: () => void;
  onLogin: (email: string, password: string) => Promise<void>;
  onRegister: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  onDashboard: () => void;
  isLoggedIn: boolean;
  userEmail?: string;
  authLoading: boolean;
  authError: string;
}

const AuthPage = ({ onBack, onLogin, onRegister, onDashboard, isLoggedIn, userEmail, authLoading, authError }: AuthPageProps) => {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (mode === "login") {
      await onLogin(email, password);
    } else {
      await onRegister(email, password, firstName, lastName);
    }
  };

  return (
    <div className="min-h-screen gradient-hero-bg flex items-center justify-center p-6">
      <div className="absolute top-20 right-20 w-80 h-80 rounded-full bg-accent/10 blur-3xl" />
      <div className="absolute bottom-20 left-20 w-96 h-96 rounded-full bg-primary/5 blur-3xl" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-5xl grid md:grid-cols-2 gap-8 relative z-10"
      >
        {/* Left side - branding */}
        <div className="hidden md:flex flex-col justify-between">
          <div>
            <Button variant="ghost" size="sm" onClick={onBack} className="mb-8 text-muted-foreground">
              <ArrowLeft className="mr-2 h-4 w-4" /> Back to Landing
            </Button>
            <h2 className="text-3xl font-heading font-bold text-foreground mb-4">Caretaker Access</h2>
            <p className="text-muted-foreground leading-relaxed">
              Secure platform access for caregivers managing patient routines, monitoring, and handover continuity.
            </p>
          </div>
          <div className="space-y-4">
            {[
              { icon: Shield, text: "Role-based access control" },
              { icon: Lock, text: "End-to-end encrypted sessions" },
              { icon: UserPlus, text: "Multi-caregiver support" },
            ].map(({ icon: Icon, text }) => (
              <div key={text} className="flex items-center gap-3 text-sm text-muted-foreground">
                <Icon className="h-5 w-5 text-accent" />
                <span>{text}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Right side - form */}
        <div className="glass-panel p-8 space-y-6">
          <Button variant="ghost" size="sm" onClick={onBack} className="md:hidden mb-2 text-muted-foreground">
            <ArrowLeft className="mr-2 h-4 w-4" /> Back
          </Button>

          {isLoggedIn ? (
            <div className="space-y-6 text-center py-8">
              <div className="w-16 h-16 rounded-2xl gradient-primary-bg flex items-center justify-center mx-auto">
                <Shield className="h-8 w-8 text-primary-foreground" />
              </div>
              <div>
                <h3 className="text-xl font-heading font-bold text-foreground mb-2">Welcome back</h3>
                <p className="text-sm text-muted-foreground">Signed in as {userEmail}</p>
              </div>
              <div className="space-y-3">
                <Button variant="hero" size="lg" className="w-full" onClick={onDashboard}>
                  Open Dashboard
                </Button>
                <Button variant="ghost" size="lg" className="w-full" onClick={onBack}>
                  Use another account
                </Button>
              </div>
            </div>
          ) : (
            <>
              <div>
                <h3 className="text-xl font-heading font-bold text-foreground mb-1">
                  {mode === "login" ? "Sign in" : "Create caregiver account"}
                </h3>
                <p className="text-sm text-muted-foreground">
                  Use one caregiver account for planning, monitoring, and handover continuity.
                </p>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                {mode === "register" && (
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-2">
                      <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">First name</Label>
                      <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required placeholder="Jane" />
                    </div>
                    <div className="space-y-2">
                      <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Last name</Label>
                      <Input value={lastName} onChange={(e) => setLastName(e.target.value)} placeholder="Doe" />
                    </div>
                  </div>
                )}
                <div className="space-y-2">
                  <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Email</Label>
                  <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required placeholder="caregiver@example.com" />
                </div>
                <div className="space-y-2">
                  <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Password</Label>
                  <Input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} placeholder="••••••••" />
                </div>

                {authError && (
                  <p className="text-sm font-medium text-destructive bg-destructive/10 rounded-lg px-4 py-2">{authError}</p>
                )}

                <Button variant="hero" size="lg" className="w-full" disabled={authLoading}>
                  {authLoading ? "Please wait..." : mode === "login" ? "Sign In" : "Create Account"}
                </Button>

                <button
                  type="button"
                  onClick={() => setMode(mode === "login" ? "register" : "login")}
                  className="w-full text-center text-sm text-muted-foreground hover:text-foreground transition-colors bg-transparent border-none cursor-pointer"
                >
                  {mode === "login" ? "Need an account? Register" : "Already have an account? Sign in"}
                </button>
              </form>
            </>
          )}
        </div>
      </motion.div>
    </div>
  );
};

export default AuthPage;
