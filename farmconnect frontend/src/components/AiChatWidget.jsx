import { useState, useRef, useEffect } from "react";
import { apiRequest } from "../api.js";

const QUICK_QUESTIONS = [
  "Which vegetables are best for sambar?",
  "How to store farm fresh tomatoes?",
  "What's in season right now?",
  "Best fruits for weight loss?",
  "How to keep spinach fresh longer?",
  "Difference between organic and regular produce?",
];

const BOT_AVATAR = "🌾";
const USER_AVATAR = "🧑";

export default function AiChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      id: 0,
      role: "bot",
      text: "👋 Hi! I'm **FarmConnect AI** — your personal farming & cooking assistant.\n\nAsk me anything about fresh produce, recipes, nutrition, or farming! 🌱",
      ts: new Date(),
    },
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [aiStatus, setAiStatus] = useState(null);
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  // Check AI status on first open
  useEffect(() => {
    if (open && aiStatus === null) {
      apiRequest("/ai/status")
        .then((data) => setAiStatus(data))
        .catch(() => setAiStatus({ configured: false }));
    }
  }, [open, aiStatus]);

  // Auto-scroll to bottom
  useEffect(() => {
    if (open) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping, open]);

  // Focus input on open
  useEffect(() => {
    if (open) setTimeout(() => inputRef.current?.focus(), 200);
  }, [open]);

  async function sendMessage(text) {
    const msg = text || input.trim();
    if (!msg || isTyping) return;
    setInput("");

    const userMsg = { id: Date.now(), role: "user", text: msg, ts: new Date() };
    setMessages((prev) => [...prev, userMsg]);
    setIsTyping(true);

    try {
      const data = await apiRequest("/ai/chat", {
        method: "POST",
        body: JSON.stringify({ message: msg }),
      });
      setMessages((prev) => [
        ...prev,
        { id: Date.now() + 1, role: "bot", text: data.reply, ts: new Date() },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          role: "bot",
          text: "⚠️ " + (err.message || "Something went wrong. Please try again."),
          ts: new Date(),
        },
      ]);
    } finally {
      setIsTyping(false);
    }
  }

  function handleKeyDown(e) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }

  // Simple markdown bold renderer
  function renderText(text) {
    const parts = text.split(/(\*\*[^*]+\*\*)/g);
    return parts.map((part, i) =>
      part.startsWith("**") && part.endsWith("**")
        ? <strong key={i}>{part.slice(2, -2)}</strong>
        : <span key={i}>{part}</span>
    );
  }

  return (
    <>
      {/* ── Floating trigger button ───────────────────────────────────── */}
      <button
        onClick={() => setOpen((o) => !o)}
        aria-label="Open FarmConnect AI Chat"
        style={{
          position: "fixed",
          bottom: "24px",
          right: "24px",
          width: "58px",
          height: "58px",
          borderRadius: "50%",
          background: "linear-gradient(135deg, #1e3a1f 0%, #2d6a2f 100%)",
          border: "none",
          cursor: "pointer",
          boxShadow: "0 4px 20px rgba(30,58,31,0.45)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "26px",
          zIndex: 9998,
          transition: "transform 0.2s",
        }}
        onMouseEnter={(e) => (e.target.style.transform = "scale(1.1)")}
        onMouseLeave={(e) => (e.target.style.transform = "scale(1)")}
      >
        {open ? "✕" : "🤖"}
      </button>

      {/* ── Chat window ───────────────────────────────────────────────── */}
      {open && (
        <div
          style={{
            position: "fixed",
            bottom: "96px",
            right: "24px",
            width: "370px",
            maxHeight: "540px",
            background: "#fff",
            borderRadius: "16px",
            boxShadow: "0 8px 40px rgba(0,0,0,0.18)",
            display: "flex",
            flexDirection: "column",
            zIndex: 9997,
            overflow: "hidden",
            border: "1px solid #e5e7eb",
            fontFamily: "Arial, sans-serif",
          }}
        >
          {/* Header */}
          <div
            style={{
              background: "linear-gradient(135deg, #1e3a1f 0%, #2d6a2f 100%)",
              color: "#fff",
              padding: "14px 16px",
              display: "flex",
              alignItems: "center",
              gap: "10px",
            }}
          >
            <span style={{ fontSize: "22px" }}>🌾</span>
            <div>
              <div style={{ fontWeight: 700, fontSize: "14px" }}>FarmConnect AI</div>
              <div style={{ fontSize: "11px", opacity: 0.8 }}>
                {aiStatus?.configured
                  ? "✅ Powered by OpenAI"
                  : "📋 Smart fallback mode"}
              </div>
            </div>
            <button
              onClick={() => setOpen(false)}
              style={{ marginLeft: "auto", background: "none", border: "none", color: "#fff", cursor: "pointer", fontSize: "18px" }}
            >✕</button>
          </div>

          {/* Messages */}
          <div style={{ flex: 1, overflowY: "auto", padding: "12px", display: "flex", flexDirection: "column", gap: "10px", background: "#f9fafb" }}>
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  display: "flex",
                  flexDirection: msg.role === "user" ? "row-reverse" : "row",
                  alignItems: "flex-end",
                  gap: "8px",
                }}
              >
                <span style={{ fontSize: "18px", flexShrink: 0 }}>
                  {msg.role === "bot" ? BOT_AVATAR : USER_AVATAR}
                </span>
                <div
                  style={{
                    maxWidth: "78%",
                    background: msg.role === "user" ? "#1e3a1f" : "#fff",
                    color: msg.role === "user" ? "#fff" : "#1f2937",
                    padding: "10px 13px",
                    borderRadius: msg.role === "user" ? "14px 14px 4px 14px" : "14px 14px 14px 4px",
                    fontSize: "13px",
                    lineHeight: "1.55",
                    boxShadow: "0 1px 4px rgba(0,0,0,0.08)",
                    whiteSpace: "pre-line",
                  }}
                >
                  {renderText(msg.text)}
                  <div style={{ fontSize: "10px", opacity: 0.5, marginTop: "4px", textAlign: msg.role === "user" ? "right" : "left" }}>
                    {msg.ts.toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}
                  </div>
                </div>
              </div>
            ))}

            {isTyping && (
              <div style={{ display: "flex", alignItems: "flex-end", gap: "8px" }}>
                <span style={{ fontSize: "18px" }}>{BOT_AVATAR}</span>
                <div style={{ background: "#fff", padding: "10px 14px", borderRadius: "14px 14px 14px 4px", boxShadow: "0 1px 4px rgba(0,0,0,0.08)" }}>
                  <span style={{ display: "flex", gap: "4px" }}>
                    {[0, 1, 2].map((i) => (
                      <span
                        key={i}
                        style={{
                          width: "7px", height: "7px", background: "#9ca3af",
                          borderRadius: "50%",
                          animation: `bounce 1.2s ${i * 0.2}s infinite`,
                        }}
                      />
                    ))}
                  </span>
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          {/* Quick questions */}
          {messages.length <= 1 && (
            <div style={{ padding: "8px 12px", background: "#f9fafb", borderTop: "1px solid #f3f4f6" }}>
              <p style={{ fontSize: "11px", color: "#6b7280", margin: "0 0 6px" }}>💡 Quick questions:</p>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "5px" }}>
                {QUICK_QUESTIONS.map((q) => (
                  <button
                    key={q}
                    onClick={() => sendMessage(q)}
                    style={{
                      background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: "20px",
                      padding: "4px 10px", fontSize: "11px", color: "#166534", cursor: "pointer",
                      transition: "background 0.15s",
                    }}
                    onMouseEnter={(e) => (e.target.style.background = "#dcfce7")}
                    onMouseLeave={(e) => (e.target.style.background = "#f0fdf4")}
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input */}
          <div style={{ display: "flex", padding: "10px 12px", gap: "8px", borderTop: "1px solid #e5e7eb", background: "#fff" }}>
            <textarea
              ref={inputRef}
              rows={1}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about produce, recipes, farming…"
              disabled={isTyping}
              style={{
                flex: 1, border: "1px solid #d1fae5", borderRadius: "10px",
                padding: "8px 12px", fontSize: "13px", resize: "none",
                fontFamily: "inherit", outline: "none", lineHeight: "1.4",
                background: isTyping ? "#f9fafb" : "#fff",
              }}
            />
            <button
              onClick={() => sendMessage()}
              disabled={!input.trim() || isTyping}
              style={{
                background: input.trim() && !isTyping ? "#1e3a1f" : "#e5e7eb",
                color: input.trim() && !isTyping ? "#fff" : "#9ca3af",
                border: "none", borderRadius: "10px", padding: "0 14px",
                cursor: input.trim() && !isTyping ? "pointer" : "not-allowed",
                fontSize: "18px", transition: "background 0.2s",
              }}
              aria-label="Send message"
            >
              ➤
            </button>
          </div>
        </div>
      )}

      {/* Bounce animation for typing dots */}
      <style>{`
        @keyframes bounce {
          0%, 60%, 100% { transform: translateY(0); }
          30% { transform: translateY(-6px); }
        }
      `}</style>
    </>
  );
}

