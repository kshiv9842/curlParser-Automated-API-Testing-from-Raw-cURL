#!/usr/bin/env node
/**
 * AI Agents bridge for curlParser scenarios.
 *
 * stdin:  ContextPack JSON
 * stdout: { "scenarios": [ ... ] }  (strict schema)
 *
 * Requires: CURSOR_API_KEY, node, npm install in ai-bridge/
 */
import { readFileSync } from "fs";
import { Agent } from "@cursor/sdk";

async function readStdin() {
  const chunks = [];
  for await (const chunk of process.stdin) {
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function extractJson(text) {
  if (!text) return null;
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start >= 0 && end > start) {
    return text.slice(start, end + 1);
  }
  return null;
}

const SYSTEM = `You are an API QA designer for FUNCTIONAL bug detection only.
Return JSON ONLY matching:
{"scenarios":[{
  "id":"snake_case",
  "objective":"string",
  "expectedResult":"string",
  "risk":"P0|P1|P2",
  "oracle":"ACCEPT|REJECT|OBSERVE",
  "mutation":{"type":"OMIT_FIELD|REPLACE_FIELD|OMIT_BODY|OMIT_HEADER|SET_CONTENT_TYPE|WRONG_METHOD|INVALID_JSON","field":null,"value":null,"header":null,"method":null},
  "rationale":"string",
  "requiresFacts":["hasBody"]
}]}

Rules:
- Propose ONLY additional cases not in alreadyCoveredCaseIds
- mutation.type MUST be from the allowlist
- field MUST exist in ContextPack.fields when using OMIT_FIELD/REPLACE_FIELD
- Do NOT invent URLs, fields, or headers
- Do NOT propose sql_injection, brute_force, xss, ssrf, ddos, exploit, or pentest
- goal is functional_bug_detection_only
- max scenarios = ContextPack.maxScenarios
- If nothing useful, return {"scenarios":[]}
`;

async function main() {
  const apiKey = process.env.CURSOR_API_KEY;
  if (!apiKey) {
    console.error("CURSOR_API_KEY missing");
    process.exit(2);
  }

  const raw = await readStdin();
  let pack;
  try {
    pack = JSON.parse(raw);
  } catch (e) {
    console.error("Invalid ContextPack JSON");
    process.exit(3);
  }

  const prompt = `${SYSTEM}\n\nContextPack:\n${JSON.stringify(pack, null, 2)}\n\nReturn JSON only.`;

  const result = await Agent.prompt(prompt, {
    apiKey,
    model: { id: "composer-2.5" },
    local: { cwd: process.cwd() },
  });

  const text = typeof result?.result === "string"
    ? result.result
    : JSON.stringify(result?.result ?? result);

  const json = extractJson(text);
  if (!json) {
    console.log(JSON.stringify({ scenarios: [] }));
    process.exit(0);
  }

  // Validate parseable
  const parsed = JSON.parse(json);
  if (!parsed.scenarios || !Array.isArray(parsed.scenarios)) {
    console.log(JSON.stringify({ scenarios: [] }));
    process.exit(0);
  }

  console.log(JSON.stringify({ scenarios: parsed.scenarios }));
}

main().catch((err) => {
  console.error(String(err?.stack || err));
  process.exit(1);
});
