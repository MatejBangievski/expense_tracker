package mk.sorsix.com.expense_tracker_backend.domain

import com.fasterxml.jackson.annotation.JsonPropertyDescription
import java.math.BigDecimal

data class GeminiApiResult(
    @get:JsonPropertyDescription(
        "True only if the input summary was usable and advice could be generated. " +
                "False if the input was empty, malformed, had no positive spending amounts, " +
                "or was otherwise unusable — in that case explain why in recommendationMessage " +
                "and leave monthlyPlan out."
    )
    val success: Boolean,

    @get:JsonPropertyDescription(
        "A short (3-5 sentence), warm, non-judgemental message from a friendly financial " +
                "consultant. If success is true: concrete advice referencing real category/" +
                "subcategory names, amounts and expense counts from the input. Never name a " +
                "specific merchant, brand or product - that information is not provided. " +
                "If success is false: a clear, plain-language explanation of why no advice could " +
                "be produced. Never invent numbers that weren't in the input."
    )
    val recommendationMessage: String,

    @get:JsonPropertyDescription(
        "Only populate when success is true. Omit entirely when success is false."
    )
    val monthlyPlan: MonthlyPlanSuggestion? = null
)

data class MonthlyPlanSuggestion(
    @get:JsonPropertyDescription(
        "Suggested spending limit per category, distributing the fixed total budget limit " +
                "given in the input across the categories you were given. Category names must " +
                "match the input exactly."
    )
    val categoryLimits: List<CategoryBudgetSuggestion>,
)

data class CategoryBudgetSuggestion(
    @get:JsonPropertyDescription(
        "Must match a category name from the input exactly."
    )
    val categoryName: String,

    @get:JsonPropertyDescription(
        "Suggested monthly spending limit for this category."
    )
    val suggestedLimit: BigDecimal,

    @get:JsonPropertyDescription(
        "Optional one-sentence reasoning for this Limit."
    )
    val reason: String? = null,
)