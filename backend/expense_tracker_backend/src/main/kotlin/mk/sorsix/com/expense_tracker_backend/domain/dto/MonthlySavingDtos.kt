package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class GenerateMonthlySavingPlanRequest(
    val budgetLimit: BigDecimal,
    val totalIncome: BigDecimal? = null,
)

data class MonthlySavingPlanResponse(
    val id: Long,
    val savingMonth: LocalDate,
    val totalBudgetLimit: BigDecimal,
    val totalIncome: BigDecimal?,
    val totalSpent: BigDecimal,
    val totalSaved: BigDecimal,
    val recommendationMessage: String?,
    val categoryLimits: List<CategoryLimitResponse> = emptyList(),
)

data class CategoryLimitResponse(
    val categoryId: Long,
    val categoryName: String,
    val monthlyLimit: BigDecimal,
    val actualSpent: BigDecimal?,
    val reason: String?,
)