import { motion } from "framer-motion";
import { ArrowRight, Download, CheckCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

interface HeroSectionProps {
  onLogin: () => void;
  onOpenDashboard: () => void;
  isLoggedIn: boolean;
}

const appScreens = [
  {
    title: "Assist Mode",
    description: "Step-by-step guided routines",
    color: "from-primary to-primary/80",
    steps: ["Morning medication", "Breakfast prep", "Mobility exercise"],
  },
  {
    title: "Task Guardian",
    description: "Smart reminders & escalation",
    color: "from-accent to-accent/80",
    steps: ["Reminder sent", "Patient confirmed", "Caregiver notified"],
  },
  {
    title: "Care Summary",
    description: "Real-time sync to dashboard",
    color: "from-success to-success/80",
    steps: ["Tasks completed: 8/10", "Mood: Positive", "Notes synced"],
  },
];

const HeroSection = ({ onLogin, onOpenDashboard, isLoggedIn }: HeroSectionProps) => {
  return (
    <section className="relative min-h-[85vh] flex items-center gradient-hero-bg overflow-hidden">
      <div className="absolute top-20 right-10 w-72 h-72 rounded-full bg-accent/10 blur-3xl" />
      <div className="absolute bottom-10 left-10 w-96 h-96 rounded-full bg-primary/5 blur-3xl" />

      <div className="container max-w-7xl mx-auto px-6 py-20">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="space-y-8"
          >
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-accent/10 border border-accent/20">
              <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
              <span className="text-sm font-semibold text-accent uppercase tracking-wider">ADAPT Care Platform</span>
            </div>

            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold font-heading text-foreground leading-[1.1] tracking-tight">
              A cleaner caregiver workflow{" "}
              <span className="text-gradient-primary">from planning to follow-through.</span>
            </h1>

            <p className="text-lg text-muted-foreground max-w-lg leading-relaxed">
              ADAPT connects the caregiver web console and Android assist experience,
              so routines, reminders, and outcomes stay in one coordinated loop.
            </p>

            <div className="flex flex-wrap gap-4">
              <Button
                variant="hero"
                size="xl"
                onClick={isLoggedIn ? onOpenDashboard : onLogin}
              >
                {isLoggedIn ? "Continue to Dashboard" : "Login"}
                <ArrowRight className="ml-1 h-5 w-5" />
              </Button>
              {!isLoggedIn && (
                <Button variant="hero-outline" size="xl" onClick={onLogin}>
                  Sign In
                </Button>
              )}
            </div>

            <div className="flex flex-wrap gap-3 pt-2">
              {["Caregiver-first operations", "Android assist integration", "Handover continuity"].map((chip) => (
                <span
                  key={chip}
                  className="px-4 py-2 rounded-full border border-border bg-card/60 text-sm text-muted-foreground font-medium"
                >
                  {chip}
                </span>
              ))}
            </div>
          </motion.div>

          {/* Mobile App Showcase */}
          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="hidden lg:flex flex-col items-center gap-6"
          >
            <div className="relative">
              <div className="w-[270px] rounded-[2.5rem] border-[6px] border-foreground/90 bg-card shadow-2xl overflow-hidden">
                <div className="h-8 bg-foreground/90 flex items-center justify-center">
                  <div className="w-20 h-4 rounded-full bg-foreground/70" />
                </div>
                <div className="gradient-primary-bg px-5 py-3">
                  <p className="text-primary-foreground/70 text-xs font-semibold uppercase tracking-widest">ADAPT Assist</p>
                  <p className="text-primary-foreground font-heading font-bold text-lg mt-1">Good Morning</p>
                  <p className="text-primary-foreground/80 text-sm">3 routines scheduled</p>
                </div>
                <div className="p-3 space-y-2 bg-background min-h-[260px]">
                  {appScreens.map((screen, i) => (
                    <motion.div
                      key={screen.title}
                      initial={{ opacity: 0, x: -10 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: 0.4 + i * 0.15 }}
                      className="glass-panel p-2.5 space-y-1.5"
                    >
                      <div className="flex items-center gap-2">
                        <div className={`w-2 h-2 rounded-full bg-gradient-to-r ${screen.color}`} />
                        <p className="text-xs font-bold text-foreground">{screen.title}</p>
                      </div>
                      <p className="text-[10px] text-muted-foreground">{screen.description}</p>
                      <div className="space-y-0.5">
                        {screen.steps.map((step, j) => (
                          <div key={j} className="flex items-center gap-1.5">
                            <CheckCircle className="w-3 h-3 text-success flex-shrink-0" />
                            <span className="text-[10px] text-foreground/80">{step}</span>
                          </div>
                        ))}
                      </div>
                    </motion.div>
                  ))}
                </div>
                <div className="h-10 border-t border-border bg-card flex items-center justify-around px-6">
                  {["Home", "Tasks", "Profile"].map((tab) => (
                    <div key={tab} className="flex flex-col items-center gap-0.5">
                      <div className="w-3 h-3 rounded bg-muted" />
                      <span className="text-[8px] text-muted-foreground">{tab}</span>
                    </div>
                  ))}
                </div>
              </div>

              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 0.8 }}
                className="absolute -right-8 top-16 glass-panel px-3 py-2 shadow-lg"
              >
                <p className="text-[10px] font-bold text-accent">Live Sync</p>
                <p className="text-[9px] text-muted-foreground">Dashboard updated</p>
              </motion.div>

              <motion.div
                initial={{ opacity: 0, scale: 0.8 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ delay: 1 }}
                className="absolute -left-6 bottom-20 glass-panel px-3 py-2 shadow-lg"
              >
                <p className="text-[10px] font-bold text-success">Offline Ready</p>
                <p className="text-[9px] text-muted-foreground">Works without WiFi</p>
              </motion.div>
            </div>

            <Button
              variant="hero"
              size="lg"
              onClick={() => window.open("#", "_blank")}
              className="w-full max-w-[270px]"
            >
              <Download className="mr-2 h-5 w-5" />
              Download APK
              <ArrowRight className="ml-1 h-4 w-4" />
            </Button>
            <p className="text-xs text-muted-foreground text-center max-w-[270px]">
              Android 8.0+ | v1.0.0 | 24 MB
            </p>
          </motion.div>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
