import { FormEvent, useRef, useEffect } from "react";
import { MessageSquare, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

type ChatMessageRole = "user" | "assistant" | "system";

interface ChatMessage {
  id: string;
  role: ChatMessageRole;
  text: string;
  actionItems?: string[];
  safetyFlags?: string[];
  confidence?: number;
  timestamp: number;
}

interface ChatPanelProps {
  messages: ChatMessage[];
  prompt: string;
  onSetPrompt: (val: string) => void;
  onSend: () => void;
  busy: boolean;
}

const ChatPanel = ({ messages, prompt, onSetPrompt, onSend, busy }: ChatPanelProps) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSend();
  };

  return (
    <div className="glass-panel p-6 space-y-4 flex flex-col" style={{ minHeight: 400 }}>
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-accent" />
        <h3 className="text-base font-heading font-bold text-foreground">AI Assistant</h3>
      </div>
      <p className="text-xs text-muted-foreground">
        Ask care-related questions. The assistant will provide action items, safety flags, and confidence scores.
      </p>

      <div className="flex-1 overflow-auto space-y-3 rounded-xl border border-border bg-background p-4 max-h-80">
        {messages.length === 0 && (
          <p className="text-xs text-muted-foreground text-center py-8">No messages yet. Start a conversation.</p>
        )}
        {messages.map((msg) => {
          const isUser = msg.role === "user";
          const isSystem = msg.role === "system";
          const hasActionItems = Array.isArray(msg.actionItems) && msg.actionItems.length > 0;
          const hasSafetyFlags = Array.isArray(msg.safetyFlags) && msg.safetyFlags.length > 0;

          return (
            <div
              key={msg.id}
              className={`rounded-xl px-4 py-3 text-sm whitespace-pre-wrap ${
                isUser
                  ? "bg-primary/10 text-foreground ml-8"
                  : isSystem
                  ? "bg-destructive/10 text-destructive"
                  : "bg-secondary text-foreground mr-8"
              }`}
            >
              {!isUser && !isSystem && <p className="text-[11px] uppercase tracking-wider opacity-70 mb-1">Assistant</p>}
              {isUser && <p className="text-[11px] uppercase tracking-wider opacity-70 mb-1">You</p>}
              {isSystem && <p className="text-[11px] uppercase tracking-wider opacity-70 mb-1">System</p>}

              <p>{msg.text}</p>

              {hasActionItems && (
                <div className="mt-3 space-y-1">
                  <p className="text-[11px] uppercase tracking-wider opacity-70">Action Items</p>
                  <ul className="list-disc list-inside text-xs space-y-1">
                    {msg.actionItems?.map((item, index) => (
                      <li key={`${msg.id}-action-${index}`}>{item}</li>
                    ))}
                  </ul>
                </div>
              )}

              {hasSafetyFlags && (
                <div className="mt-3 flex flex-wrap gap-1.5">
                  {msg.safetyFlags?.map((flag, index) => (
                    <span
                      key={`${msg.id}-flag-${index}`}
                      className="px-2 py-1 rounded-md bg-accent/15 text-accent text-[10px] font-semibold"
                    >
                      {flag}
                    </span>
                  ))}
                </div>
              )}

              {typeof msg.confidence === "number" && Number.isFinite(msg.confidence) && (
                <p className="mt-2 text-[11px] opacity-75">Confidence: {Math.round(msg.confidence * 100)}%</p>
              )}
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input
          value={prompt}
          onChange={(e) => onSetPrompt(e.target.value)}
          placeholder="Ask about patient care, protocols..."
          className="flex-1"
        />
        <Button type="submit" variant="accent" size="icon" disabled={busy || !prompt.trim()}>
          <Send className="h-4 w-4" />
        </Button>
      </form>
    </div>
  );
};

export default ChatPanel;
