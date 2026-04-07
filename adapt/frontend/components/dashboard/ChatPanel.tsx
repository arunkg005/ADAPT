import { FormEvent, useRef, useEffect } from "react";
import { MessageSquare, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface ChatPanelProps {
  messages: string[];
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
        {messages.map((msg, i) => {
          const isUser = msg.startsWith("You:");
          const isSystem = msg.startsWith("System:");
          return (
            <div
              key={i}
              className={`rounded-xl px-4 py-3 text-sm whitespace-pre-wrap ${
                isUser
                  ? "bg-primary/10 text-foreground ml-8"
                  : isSystem
                  ? "bg-destructive/10 text-destructive"
                  : "bg-secondary text-foreground mr-8"
              }`}
            >
              {msg}
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
