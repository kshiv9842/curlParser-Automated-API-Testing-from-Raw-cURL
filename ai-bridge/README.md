# AI Bridge (AI Agents)

Node helper that turns a `ContextPack` into constrained scenario JSON for the Java validator/executor.

## Setup

```bash
cd ai-bridge
npm install
```

Set `CURSOR_API_KEY` in the environment (Render env vars / local shell).

## Run (usually via Java)

Java generator runs:

```bash
node generate.mjs
```

stdin = ContextPack JSON → stdout = `{ "scenarios": [...] }`.

If the bridge is unavailable, the Java layer falls back to **Smart Assist** (field-shape rules).
