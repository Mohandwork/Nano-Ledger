package com.mo.miniledger.ai

import android.graphics.Bitmap
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.mo.miniledger.BuildConfig
import com.mo.miniledger.ai.model.ExtractedTransaction
import kotlinx.serialization.json.Json

class ReceiptProcessor {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    val adkAgent = LlmAgent(
        name = "testing Agent",
        model = Gemini(
            name = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        ),
    )

    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun processReceipt(bitmap: Bitmap, context: String? = null): ExtractedTransaction? {
        val prompt = """
            Analyze this receipt image and extract ALL relevant information as a flat map of key-value pairs.
            Additionally, perform a corporate compliance check on individual line items.
            
            ${if (context != null) "Additional Context (e.g., Calendar Events): $context" else ""}
            
            Strictly follow this structure in your JSON response:
            
            1. fields: (Map<String, String>) A flat dictionary of ALL data found on the receipt.
                Mandatory keys (guess if not explicit):
                - "Merchant": The name of the business ( ALWAYS first ).
                - "Business Reason": Based on the receipt and the provided context, determine the reason for this transaction ( ALWAYS second ). 
                  Start the value with one of these categories: [Client], [Company], or [Personal]. 
                  Example: "[Client] Dinner with Acme Corp representative" or "[Personal] Weekly grocery run".
                - "Amount": The total value of the transaction (number only).
                - "Type": Either "Expense" or "Income".
                - "Category": Choose the best from: Food, Dining, Groceries, Travel, Transportation, Shopping, Entertainment, Health, Utilities, Salary, Investment, Education, Housing.
                - "Date": The date as it appears on the receipt.
                
                Itemized Extraction:
                - Extract each individual line item separately. 
                - Use keys like "Item 1", "Item 1 Price", "Item 2", "Item 2 Price" etc.
                - DO NOT pack all items into a single string. Every identifiable line item should have its own set of keys in this map.
                
                Other: Include everything else (Tax, Tip, Table #, Flight #, etc.).

            - items: (List of Objects) A structured breakdown for compliance analysis of individual items:
                - itemId: Unique ID (e.g., "ITEM_1").
                - description: Name of the item.
                - totalPrice: Price for this line item.
                - category: "FOOD", "TRANSPORT", "LODGING", or "OTHER".
                - policyStatus: "APPROVED", "VIOLATION_RESTRICTED" (Alcohol, Tobacco, Luxury), or "VIOLATION_LIMIT_EXCEEDED" (Individual item > $500).
                - violationReason: Reason if not APPROVED.
                - inlineActions: Exactly 2 recommended resolution actions for violations (e.g., "Exclude Item", "Mark Personal"). If no violations, return an empty array [].
                    - Each action: actionId, buttonText, uiStyleHint ("PRIMARY", "SECONDARY", "OUTLINED"), resultingValueModifier (numeric).

            - flaggedItems: (List<String>) Any prohibited or suspicious items found (Alcohol, Tobacco, etc.)
            - suggestedTimestamp: (Long) The transaction date converted to milliseconds since epoch.
            
            Return ONLY the JSON object. No conversational text.
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            val jsonString = response.text?.replace("```json", "")?.replace("```", "")?.trim()
            if (jsonString != null) {
                json.decodeFromString<ExtractedTransaction>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
