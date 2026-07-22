# AI Bridge (Cursor SDK)

Optional generator for **AI scenarios**. The Java app validates every proposal before execution.

## Setup

```bash
cd ai-bridge
npm install
export CURSOR_API_KEY=your_key
```

## How it is called

Java `CursorSdkScenarioGenerator` runs:

```bash
node ai-bridge/generate.mjs
```

stdin = ContextPack JSON → stdout = `{ "scenarios": [ ... ] }`

If `CURSOR_API_KEY` is missing, the app falls back to **shape-based** proposals (per-field omit / login-ish replaces), still through the same validator + executor.
