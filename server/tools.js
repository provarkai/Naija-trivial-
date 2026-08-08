// Mirrors the tool metadata in the Android app's ToolType.kt. Kept as a
// separate list here (rather than shared code) since the client and server
// are different runtimes -- if you add a tool on the client, add its prompt
// here too.

export const TOOLS = {
  business_plan: {
    title: "AI Business Plan Generator",
    instructions:
      "Write a structured business plan with these sections, each as a " +
      "clear heading followed by 1-3 short paragraphs or bullet points: " +
      "Executive Summary, Target Market, Problem & Solution, Revenue Model, " +
      "Next Steps. Ground everything in the details provided below -- don't " +
      "invent unrelated facts."
  },
  proposal: {
    title: "AI Proposal Generator",
    instructions:
      "Write a client-ready project proposal with these sections: Overview, " +
      "Scope of Work, Timeline, Investment, Next Steps. Professional, " +
      "concise, and directly usable -- this will be sent to the client as-is."
  },
  invoice_receipt: {
    title: "AI Invoice & Receipt Generator",
    instructions:
      "Format a clean invoice with: From, Bill To, an itemized list of " +
      "items/services, Total Due, and Due Date. Plain text layout suitable " +
      "for a business document, no markdown tables."
  },
  social_media_content: {
    title: "AI Social Media Content Generator",
    instructions:
      "Write 3 short social media post options for the given brand, " +
      "platform, and topic, matching the requested tone. Label them " +
      "Option 1/2/3 and vary the style (announcement, story-driven, short & " +
      "punchy)."
  },
  whatsapp_reply: {
    title: "AI WhatsApp Reply Generator",
    instructions:
      "Write a single ready-to-send WhatsApp reply to the customer's " +
      "message, matching the requested tone and reflecting any business " +
      "context given. Keep it natural and brief, the way a real business " +
      "owner would text back."
  }
};
