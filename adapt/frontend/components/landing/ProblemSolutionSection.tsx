import { motion } from "framer-motion";
import { X, Check } from "lucide-react";

const problems = [
  "Care instructions get split across messages, notes, and separate tools.",
  "Patient task progress is hard to audit during caregiver handovers.",
  "Missed steps are often discovered after the situation escalates.",
];

const solutions = [
  "Create and publish routines from one caregiver dashboard.",
  "Deliver patient-friendly guided flows in the Android app.",
  "Feed outcomes back into web analysis for quick, informed decisions.",
];

const ProblemSolutionSection = () => {
  return (
    <section className="py-24 bg-background">
      <div className="container max-w-7xl mx-auto px-6">
        <div className="grid md:grid-cols-2 gap-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="glass-panel p-8 space-y-6"
          >
            <h3 className="text-2xl font-heading font-bold text-foreground">Problem we solve</h3>
            <div className="space-y-4">
              {problems.map((p) => (
                <div key={p} className="flex gap-3 items-start">
                  <div className="w-6 h-6 rounded-full bg-destructive/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <X className="h-3.5 w-3.5 text-destructive" />
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">{p}</p>
                </div>
              ))}
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="glass-panel p-8 space-y-6 border-accent/20"
          >
            <h3 className="text-2xl font-heading font-bold text-foreground">Our approach</h3>
            <div className="space-y-4">
              {solutions.map((s) => (
                <div key={s} className="flex gap-3 items-start">
                  <div className="w-6 h-6 rounded-full bg-success/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                    <Check className="h-3.5 w-3.5 text-success" />
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">{s}</p>
                </div>
              ))}
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
};

export default ProblemSolutionSection;
