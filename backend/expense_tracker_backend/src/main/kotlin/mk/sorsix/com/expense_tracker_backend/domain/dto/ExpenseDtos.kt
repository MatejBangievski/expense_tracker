package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CreateExpenseRequest(
    val categoryId: Long,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val description: String? = null,
    val planId: Long? = null,
    val confirmOverBudget: Boolean? = false,
)

data class UpdateExpenseRequest(
    val categoryId: Long,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val description: String? = null,
    val planId: Long? = null,
    val confirmOverBudget: Boolean? = false,
)

data class ExpenseResponse(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val planId: Long? = null,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
    val description: String?,
)

data class ExpenseFilter(
    val categoryName: String? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val search: String? = null,
)

data class TopCategoryResponse(
    val categoryId: Long,
    val categoryName: String,
    val totalSpent: BigDecimal,
    val share: Double,
)

data class TotalResponse(
    val total: BigDecimal,
)

interface CategoryTotalProjection {
    val categoryId: Long
    val categoryName: String
    val totalSpent: BigDecimal
}
