const fs = require('fs');
const path = require('path');
const Anthropic = require('@anthropic-ai/sdk');

// AGENT_PROMPT.md lives one level up from backend/ (in fitcoach-pro/).
const AGENT_PROMPT_PATH = path.join(__dirname, '..', '..', 'AGENT_PROMPT.md');
// The actual plan (transcribed from the uploaded PDF) - authoritative source
// of truth per AGENT_PROMPT.md's "Source Hierarchy". Injected as a separate
// cached system block on every call, not re-typed into the prompt itself.
const WORKOUT_PLAN_PATH = path.join(__dirname, '..', 'data', 'workout_plan.md');

let anthropic = null;
function getClient() {
  if (!anthropic) {
    if (!process.env.ANTHROPIC_API_KEY) {
      throw new Error('ANTHROPIC_API_KEY is not set - copy .env.example to .env and fill it in.');
    }
    anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY });
  }
  return anthropic;
}

let cachedSystemPrompt = null;
function getSystemPrompt() {
  if (cachedSystemPrompt) return cachedSystemPrompt;

  const raw = fs.readFileSync(AGENT_PROMPT_PATH, 'utf8');
  // AGENT_PROMPT.md is a doc, not just a prompt - it has a "Notes for whoever
  // wires this up" section and framing text before/after the actual system
  // prompt. Only the "## SYSTEM PROMPT" section should go to the model.
  const startMarker = '## SYSTEM PROMPT';
  const endMarker = '## Structured output contract';
  const startIdx = raw.indexOf(startMarker);
  const endIdx = raw.indexOf(endMarker);

  if (startIdx === -1) {
    // Fallback: send the whole file rather than fail outright.
    cachedSystemPrompt = raw;
  } else {
    const body = endIdx > startIdx ? raw.slice(startIdx, endIdx) : raw.slice(startIdx);
    cachedSystemPrompt = body.trim();
  }

  // The structured-output contract is part of what the model needs to know
  // to emit the JSON block correctly, so append it explicitly.
  const contractStart = raw.indexOf('## Structured output contract');
  if (contractStart !== -1) {
    const notesStart = raw.indexOf('## Notes for whoever wires this up');
    const contractBlock =
      notesStart > contractStart
        ? raw.slice(contractStart, notesStart)
        : raw.slice(contractStart);
    cachedSystemPrompt += '\n\n' + contractBlock.trim();
  }

  return cachedSystemPrompt;
}

let cachedWorkoutPlan = null;
function getWorkoutPlan() {
  if (!cachedWorkoutPlan) {
    cachedWorkoutPlan = fs.readFileSync(WORKOUT_PLAN_PATH, 'utf8');
  }
  return cachedWorkoutPlan;
}

async function askCoach({ userMessage, contextBlock }) {
  const client = getClient();

  // Two separate cached blocks: the persona (rarely changes) and the plan
  // (essentially static for the whole 18 months). Splitting them means an
  // edit to one doesn't invalidate the cache on the other.
  const system = [
    {
      type: 'text',
      text: getSystemPrompt(),
      cache_control: { type: 'ephemeral' },
    },
    {
      type: 'text',
      text: `# ATTACHED WORKOUT PLAN (authoritative source of truth - study before answering, never contradict)\n\n${getWorkoutPlan()}`,
      cache_control: { type: 'ephemeral' },
    },
  ];

  const response = await client.messages.create({
    // Check console.anthropic.com/docs for the current recommended model
    // string before relying on this - model names change over time.
    model: process.env.CLAUDE_MODEL || 'claude-sonnet-5',
    max_tokens: 1500,
    system,
    messages: [
      {
        role: 'user',
        content: `${contextBlock}\n\n---\n\n${userMessage}`,
      },
    ],
  });

  const textBlock = response.content.find((block) => block.type === 'text');
  return textBlock ? textBlock.text : '';
}

module.exports = { askCoach };
