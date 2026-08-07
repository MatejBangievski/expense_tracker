package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CreateBudgetRequest(
    val categoryId: Long,
    val budgetMonth: LocalDate,
    val monthlyLimit: BigDecimal,
    val reason: String? = null
)

data class UpdateBudgetRequest(
    val monthlyLimit: BigDecimal,
    val reason: String? = null
)

data class BudgetResponse(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val budgetMonth: LocalDate,
    val monthlyLimit: BigDecimal,
    val actualSpent: BigDecimal?,
    val reason: String?
)