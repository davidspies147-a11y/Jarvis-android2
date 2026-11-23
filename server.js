// Minimal Node.js backend. Keeps your API key off the Android device.
import express from "express";

const app = express();
app.use(express.json());

app.post("/chat", async (req, res) => {
  try {
    const message = String(req.body?.message ?? "").trim();
    if (!message) return res.status(400).json({ error: "Message is required" });

    const response = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: process.env.OPENAI_MODEL || "gpt-5",
        instructions: "You are JARVIS, a polished, intelligent, calm personal assistant. Be concise, useful, respectful, and lightly witty. Never claim to have performed actions you cannot perform.",
        input: message
      })
    });

    const data = await response.json();
    if (!response.ok) return res.status(response.status).json({ error: data.error?.message || "AI request failed" });

    const reply = (data.output || [])
      .flatMap(item => item.content || [])
      .filter(part => part.type === "output_text")
      .map(part => part.text)
      .join("\n");

    res.json({ reply: reply || "I could not form a response." });
  } catch (error) {
    res.status(500).json({ error: "Server error" });
  }
});

const port = process.env.PORT || 3000;
app.listen(port, () => console.log(`JARVIS backend listening on ${port}`));
