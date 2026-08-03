/**
 * The coach persona (see ../AGENT_PROMPT.md, "Structured output contract") ends
 * loggable responses with a fenced ```json block. This strips it out of the
 * human-readable reply and parses it separately so the caller can log it and
 * the user never sees the raw JSON.
 */
function extractStructuredLog(rawText) {
  const match = rawText.match(/```json\s*([\s\S]*?)```/);
  if (!match) {
    return { humanText: rawText.trim(), structured: null };
  }

  let structured = null;
  try {
    structured = JSON.parse(match[1]);
  } catch (err) {
    // Malformed JSON from the model - don't crash the request over it,
    // just log it server-side and return no structured data for this turn.
    console.warn('Failed to parse structured output block:', err.message);
  }

  const humanText = rawText.slice(0, match.index).trim();
  return { humanText, structured };
}

module.exports = { extractStructuredLog };
