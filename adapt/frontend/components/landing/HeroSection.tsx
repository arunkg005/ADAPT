import { motion } from "framer-motion";
import { ArrowRight, Shield } from "lucide-react";
import { Button } from "@/components/ui/button";

interface HeroSectionProps {
  onGetStarted: () => void;
  onSignIn: () => void;
  isLoggedIn: boolean;
}

const HeroSection = ({ onGetStarted, onSignIn, isLoggedIn }: HeroSectionProps) => {
  return (
    <section className="relative min-h-[85vh] flex items-center gradient-hero-bg overflow-hidden">
      {/* Decorative circles */}
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
              <Button variant="hero" size="xl" onClick={onGetStarted}>
                {isLoggedIn ? "Continue to Dashboard" : "Get Started"}
                <ArrowRight className="ml-1 h-5 w-5" />
              </Button>
              {!isLoggedIn && (
                <Button variant="hero-outline" size="xl" onClick={onSignIn}>
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

          <motion.div
            initial={{ opacity: 0, x: 30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="hidden lg:grid gap-4"
          >
            {/* Visual cards */}
            <div className="glass-panel p-6 space-y-4">
              <p className="text-xs font-bold uppercase tracking-widest text-accent">How it runs</p>
              <h3 className="text-xl font-heading font-bold text-foreground">Plan, Guide, Review</h3>
              <div className="space-y-3">
                {["Routine planned", "Assist mode active", "Care summary synced"].map((step, i) => (
                  <div key={step} className="flex items-center gap-3">
                    <span className="w-8 h-8 rounded-lg gradient-primary-bg flex items-center justify-center text-primary-foreground text-sm font-bold">
                      {i + 1}
                    </span>
                    <span className="text-sm text-foreground font-medium">{step}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="glass-panel p-5 space-y-2">
                <p className="text-xs font-bold uppercase tracking-widest text-accent">Live context</p>
                <h4 className="text-lg font-heading font-bold text-foreground">Today at a glance</h4>
                <div className="space-y-3 pt-2">
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">Routines completed</p>
                    <div className="h-2.5 rounded-full bg-muted overflow-hidden">
                      <div className="h-full rounded-full gradient-primary-bg w-3/4" />
                    </div>
                  </div>
                  <div>
                    <p className="text-xs text-muted-foreground mb-1">Assist sessions synced</p>
                    <div className="h-2.5 rounded-full bg-muted overflow-hidden">
                      <div className="h-full rounded-full gradient-accent-bg w-1/2" />
                    </div>
                  </div>
                </div>
              </div>
              <div className="glass-panel p-5 flex flex-col items-center justify-center text-center space-y-2">
                <div className="w-12 h-12 rounded-xl gradient-accent-bg flex items-center justify-center">
                  <Shield className="h-6 w-6 text-accent-foreground" />
                </div>
                <p className="text-sm font-semibold text-foreground">HIPAA Ready</p>
                <p className="text-xs text-muted-foreground">Role-based security</p>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
