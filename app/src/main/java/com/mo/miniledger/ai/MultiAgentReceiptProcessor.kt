package com.mo.miniledger.ai

import android.graphics.Bitmap
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.agents.ParallelAgent
import com.google.adk.kt.agents.SequentialAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.types.Blob
import com.google.adk.kt.types.Content
import com.google.adk.kt.types.Part
import com.mo.miniledger.BuildConfig
import com.mo.miniledger.ai.model.ExtractedTransaction
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

/**
 * Multi-agent receipt processor that uses a **Sequential → Parallel → Sequential** pipeline:
 *
 * ```
 * SequentialAgent (orchestrator)
 *   ├── receipt_extraction_agent          ← Step 1: parse image, output raw fields & items
 *   ├── ParallelAgent (parallel_analysis) ← Step 2: two agents run concurrently
 *   │     ├── compliance_agent            ← 2a: policy-check each line item
 *   │     └── enrichment_agent            ← 2b: validate/enrich metadata fields
 *   └── assembly_agent                    ← Step 3: merge all outputs → ExtractedTransaction JSON
 * ```
 *
 * The [ParallelAgent] fires [compliance_agent] and [enrichment_agent] at the same time using
 * Kotlin coroutine `merge()`, cutting the total LLM round-trips from 3 sequential calls down to
 * 2 "wall-clock" steps.
 */
class MultiAgentReceiptProcessor {

    private val geminiModel = Gemini(
        name = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 – Receipt Extraction  (sequential, must run first)
    // ─────────────────────────────────────────────────────────────────────────
    private val receiptExtractionAgent = LlmAgent(
        name = "receipt_extraction_agent",
        model = geminiModel,
        description = "Extracts all visible data from a receipt image: merchant, totals, line " +
                "items, tax, tip, and any other printed fields.",
        instruction = Instruction(
            """
            You are a receipt-data extraction specialist.
            Analyze the receipt image supplied earlier in this conversation and extract ALL visible
            information.
            
            Produce a single JSON object with these top-level keys:
            
            1. "fields" – (object, string values only) Flat key/value map of every data point.
               Mandatory keys (estimate if not explicitly shown):
               • "Merchant"        – Business name (ALWAYS first)
               • "Business Reason" – Category-prefixed spending reason. Start with one of:
                                     [Client], [Company], or [Personal].
                                     Example: "[Client] Dinner with Acme Corp representative"
               • "Amount"          – Total charge, digits only (e.g. "42.50")
               • "Type"            – "Expense" or "Income"
               • "Category"        – Best match: Food, Dining, Groceries, Travel, Transportation,
                                     Shopping, Entertainment, Health, Utilities, Salary,
                                     Investment, Education, Housing
               • "Date"            – Date exactly as printed
               Also add each line item as separate keys: "Item 1", "Item 1 Price", "Item 2", …
               Include Tax, Tip, Table #, Flight #, and all other visible data.
            
            2. "items" – (array) One entry per line item:
               { "itemId": "ITEM_1", "description": "…", "totalPrice": 0.0,
                 "category": "FOOD|TRANSPORT|LODGING|OTHER" }
            
            3. "suggestedTimestamp" – (number) Receipt date in milliseconds since Unix epoch.
            
            Return ONLY the raw JSON object. No markdown, no prose.
            """.trimIndent()
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2a – Compliance Analysis  (runs in parallel with enrichment_agent)
    // ─────────────────────────────────────────────────────────────────────────
    private val complianceAgent = LlmAgent(
        name = "compliance_agent",
        model = geminiModel,
        description = "Evaluates each extracted line item against corporate expense policy.",
        instruction = Instruction(
            """
            You are a corporate expense-policy compliance officer.
            The conversation history contains the receipt extraction result.
            Using the "items" list from that result, evaluate every item.
            
            Policy rules:
            • VIOLATION_RESTRICTED      – Alcohol, Tobacco, or Luxury item.
            • VIOLATION_LIMIT_EXCEEDED  – Single line item > ${'$'}500.
            • APPROVED                  – Everything else.
            
            Output ONE JSON object:
            {
              "complianceItems": [
                {
                  "itemId":          "ITEM_1",
                  "policyStatus":    "APPROVED|VIOLATION_RESTRICTED|VIOLATION_LIMIT_EXCEEDED",
                  "violationReason": "Explanation, or null if APPROVED",
                  "inlineActions":   [
                    { "actionId": "action_exclude", "buttonText": "Exclude Item",
                      "uiStyleHint": "PRIMARY", "resultingValueModifier": -42.50 },
                    { "actionId": "action_personal", "buttonText": "Mark Personal",
                      "uiStyleHint": "OUTLINED", "resultingValueModifier": 0 }
                  ]
                }
              ],
              "flaggedItems": ["item name", …]
            }
            
            • APPROVED  → "inlineActions" must be [].
            • Violations → provide EXACTLY 2 resolution actions.
            
            Return ONLY the raw JSON object. No markdown, no prose.
            """.trimIndent()
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2b – Metadata Enrichment  (runs in parallel with compliance_agent)
    // ─────────────────────────────────────────────────────────────────────────
    private val enrichmentAgent = LlmAgent(
        name = "enrichment_agent",
        model = geminiModel,
        description = "Validates and enriches the extracted receipt metadata: normalises the " +
                "category, refines the business reason, and verifies the timestamp.",
        instruction = Instruction(
            """
            You are a financial-data enrichment specialist.
            The conversation history contains the receipt extraction result.
            Using the "fields" map and "suggestedTimestamp" from that result, produce enriched
            metadata.
            
            Tasks:
            1. Validate and if necessary correct the "Category" value – it must be exactly one of:
               Food, Dining, Groceries, Travel, Transportation, Shopping, Entertainment, Health,
               Utilities, Salary, Investment, Education, Housing.
            2. Refine the "Business Reason" if the context or merchant name implies a clearer
               purpose.  Keep the [Client]/[Company]/[Personal] prefix.
            3. Verify that "suggestedTimestamp" is a plausible Unix-epoch millisecond value.
               If it looks wrong (e.g. zero, year < 2000, year > 2100), attempt to re-derive it
               from the "Date" field.
            4. Determine whether the transaction is an expense (true) or income (false) and add
               an "isExpense" boolean.
            
            Output ONE JSON object:
            {
              "enrichedFields": {
                "Category":        "corrected value",
                "Business Reason": "refined value",
                "Merchant":        "normalised merchant name"
              },
              "suggestedTimestamp": 1718000000000,
              "isExpense":         true
            }
            
            Return ONLY the raw JSON object. No markdown, no prose.
            """.trimIndent()
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 – Parallel wrapper (fires compliance + enrichment simultaneously)
    // ─────────────────────────────────────────────────────────────────────────
    private val parallelAnalysis = ParallelAgent(
        name = "parallel_analysis",
        description = "Runs compliance checking and metadata enrichment concurrently.",
        subAgents = listOf(complianceAgent, enrichmentAgent)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Step 3 – Final Assembly  (sequential, must run last)
    // ─────────────────────────────────────────────────────────────────────────
    private val assemblyAgent = LlmAgent(
        name = "assembly_agent",
        model = geminiModel,
        description = "Merges extraction, compliance, and enrichment outputs into the final " +
                "ExtractedTransaction JSON.",
        instruction = Instruction(
            """
            You are a data-assembly specialist.
            The conversation history contains THREE prior results:
              • Receipt extraction  (fields, items, suggestedTimestamp)
              • Compliance analysis (complianceItems, flaggedItems)
              • Metadata enrichment (enrichedFields, suggestedTimestamp, isExpense)
            
            Merge them into ONE final JSON object matching this EXACT structure:
            {
              "fields": {
                "Merchant":        "…",
                "Business Reason": "…",
                "Amount":          "…",
                "Type":            "…",
                "Category":        "…",
                "Date":            "…"
                … (all other fields; prefer enriched values over raw values when they differ)
              },
              "items": [
                {
                  "itemId":          "ITEM_1",
                  "description":     "…",
                  "totalPrice":      0.0,
                  "category":        "FOOD|TRANSPORT|LODGING|OTHER",
                  "policyStatus":    "APPROVED|VIOLATION_RESTRICTED|VIOLATION_LIMIT_EXCEEDED",
                  "violationReason": null,
                  "inlineActions":   []
                }
              ],
              "flaggedItems":       [],
              "suggestedTimestamp": 0
            }
            
            Rules:
            • Keep ALL "fields" key/value pairs from the extraction.
            • Override "Category", "Business Reason", and "Merchant" with enriched values if
              provided.
            • Use enrichment's "suggestedTimestamp" when it differs from extraction's value.
            • Merge each item: match on itemId, attach policyStatus / violationReason /
              inlineActions from compliance; default to APPROVED if no compliance entry exists.
            • Use flaggedItems from the compliance result.
            
            Return ONLY the raw JSON object. No markdown, no prose.
            """.trimIndent()
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Top-level orchestrator
    // ─────────────────────────────────────────────────────────────────���───────

    /**
     * Pipeline:
     *   extraction (seq) → compliance + enrichment (parallel) → assembly (seq)
     */
    private val orchestrator = SequentialAgent(
        name = "receipt_orchestrator",
        description = "Runs the full receipt-processing pipeline: extract → analyse (parallel) → assemble.",
        subAgents = listOf(receiptExtractionAgent, parallelAnalysis, assemblyAgent)
    )

    private val runner = InMemoryRunner(
        agent = orchestrator,
        appName = "Nano Ledger"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Processes a receipt bitmap through the hybrid multi-agent pipeline.
     *
     * **Wall-clock steps:**
     * 1. [receiptExtractionAgent] extracts raw data from the image.
     * 2. [complianceAgent] **and** [enrichmentAgent] run *concurrently* on that data.
     * 3. [assemblyAgent] merges all outputs into a final [ExtractedTransaction].
     *
     * @param bitmap   The receipt image captured by the camera.
     * @param context  Optional extra context (e.g. calendar events) used to refine the
     *                 "Business Reason" field.
     * @return         The fully assembled [ExtractedTransaction], or `null` on failure.
     */
    suspend fun processReceipt(bitmap: Bitmap, context: String? = null): ExtractedTransaction? {
        val imageBytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }

        val contextNote = if (!context.isNullOrBlank()) "\n\nAdditional Context: $context" else ""

        val userMessage = Content(
            role = "user",
            parts = listOf(
                Part(inlineData = Blob(mimeType = "image/jpeg", data = imageBytes)),
                Part(text = "Please analyze this receipt image and extract all relevant data.$contextNote")
            )
        )

        val sessionId = "receipt_session_${System.currentTimeMillis()}"

        return try {
            val events = runner.runAsync(
                userId = "user",
                sessionId = sessionId,
                newMessage = userMessage
            ).toList()

            // Collect the final text response from the assembly agent
            val assemblyOutput = events
                .filter { it.author == assemblyAgent.name }
                .lastOrNull { it.content?.parts?.any { p -> p.text != null } == true }
                ?.content
                ?.parts
                ?.firstOrNull { it.text != null }
                ?.text

            assemblyOutput
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()
                ?.let { jsonString ->
                    json.decodeFromString<ExtractedTransaction>(jsonString)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

