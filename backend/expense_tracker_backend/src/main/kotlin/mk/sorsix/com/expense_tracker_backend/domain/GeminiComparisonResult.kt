package mk.sorsix.com.expense_tracker_backend.domain

import com.fasterxml.jackson.annotation.JsonPropertyDescription


data class GeminiComparisonResult(
    @get:JsonPropertyDescription(
        "True only if both spending summaries were usable and a comparison could be made. " +
                "False if either period had no categories and no positive spending, or the input " +
                "was otherwise empty or nonsensical — in that case explain why in comparisonMessage."
    )
    val success: Boolean,

    @get:JsonPropertyDescription(
        "A short (3-6 sentence), warm, non-judgemental comparison written in second person and " +
                "anchored to where the user stands right now (e.g. \"As of now, you've spent...\"). " +
                "When the current period is still in progress, compare PACE rather than raw totals — " +
                "a partial month naturally shows lower totals than a complete one. Refer only to " +
                "category and subcategory names, amounts and expense counts from the input. Never " +
                "name a specific merchant, brand or product - that information is not provided. " +
                "If success is false: a clear, plain-language explanation of why no comparison " +
                "could be produced. Never invent numbers that weren't in the input."
    )
    val comparisonMessage: String,
)