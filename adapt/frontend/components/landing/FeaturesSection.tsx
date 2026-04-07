import { motion } from "framer-motion";
import { ClipboardList, AlertTriangle, BarChart3, MessageSquare, Smartphone, RefreshCw } from "lucide-react";

const features = [
  {
    icon: ClipboardList,
    title: "Task Lab",
    description: "Create and publish care routines from templates or AI-generated drafts. Assign to patients instantly.",
  },
  {
    icon: AlertTriangle,
    title: "Alert Management",
    description: "Real-time alerts with severity tracking and acknowledgement workflows to prevent escalations.",
  },
  {
    icon: BarChart3,
    title: "Analysis Dashboard",
    description: "Patient summaries, telemetry insights, and trend analysis in configurable time windows.",
  },
  {
    icon: MessageSquare,
    title: "AI Assistant",
    description: "Context-aware chat with action items, safety flags, and confidence scoring for care decisions.",
  },
  {
    icon: Smartphone,
    title: "Android Assist Mode",
    description: "Step-by-step patient guidance with reminder escalation and live status sync back to dashboard.",
  },
  {
    icon: RefreshCw,
    title: "Handover Continuity",
    description: "Seamless shift transitions with complete audit trails and care progress visibility.",
  },
];

const FeaturesSection = () => {
  return (
    <section className="py-24 bg-background">
      <div className="container max-w-7xl mx-auto px-6">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5 }}
          className="text-center mb-16 space-y-4"
        >
          <p className="text-sm font-bold uppercase tracking-widest text-accent">Platform Capabilities</p>
          <h2 className="text-3xl md:text-4xl font-heading font-bold text-foreground">
            Everything caregivers need, unified
          </h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            From planning routines to reviewing outcomes — one coordinated platform
            for web and mobile care delivery.
          </p>
        </motion.div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 16 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: i * 0.08 }}
              className="glass-panel p-6 space-y-4 hover:shadow-lg transition-shadow duration-300 group"
            >
              <div className="w-12 h-12 rounded-xl bg-accent/10 flex items-center justify-center group-hover:bg-accent/20 transition-colors">
                <feature.icon className="h-6 w-6 text-accent" />
              </div>
              <h3 className="text-lg font-heading font-bold text-foreground">{feature.title}</h3>
              <p className="text-sm text-muted-foreground leading-relaxed">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default FeaturesSection;
