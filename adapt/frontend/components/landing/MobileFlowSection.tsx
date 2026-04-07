import { motion } from "framer-motion";
import { Play, Bell, Activity } from "lucide-react";

const flows = [
  {
    icon: Play,
    eyebrow: "Assist Mode",
    title: "Step-by-step guidance",
    description: "Patient sees one clear instruction at a time.",
  },
  {
    icon: Bell,
    eyebrow: "Task Guardian",
    title: "Reminder escalation",
    description: "Missed confirmations can trigger caregiver follow-up.",
  },
  {
    icon: Activity,
    eyebrow: "Status Sync",
    title: "Live care visibility",
    description: "Task and signal updates return to the dashboard instantly.",
  },
];

const MobileFlowSection = () => {
  return (
    <section className="py-24 bg-secondary/30">
      <div className="container max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="text-center mb-16 space-y-4"
        >
          <p className="text-sm font-bold uppercase tracking-widest text-accent">Android Assist Flow</p>
          <h2 className="text-3xl md:text-4xl font-heading font-bold text-foreground">
            Patient-facing mobile experience
          </h2>
        </motion.div>

        <div className="grid md:grid-cols-3 gap-8">
          {flows.map((flow, i) => (
            <motion.div
              key={flow.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="glass-panel p-8 text-center space-y-4"
            >
              <div className="w-14 h-14 rounded-2xl gradient-primary-bg flex items-center justify-center mx-auto">
                <flow.icon className="h-7 w-7 text-primary-foreground" />
              </div>
              <p className="text-xs font-bold uppercase tracking-widest text-accent">{flow.eyebrow}</p>
              <h3 className="text-xl font-heading font-bold text-foreground">{flow.title}</h3>
              <p className="text-sm text-muted-foreground">{flow.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default MobileFlowSection;
