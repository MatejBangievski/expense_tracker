package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CategorySummary(
    val categoryName: String,
    val totalAmount: BigDecimal,
    val subcategories: List<SubcategorySummary>,
)

data class SubcategorySummary(
    val subcategoryName: String,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
    val expenses: List<ExpenseSummary>,
)

data class ExpenseSummary(
    val description: String?,
    val amount: BigDecimal,
    val expenseDate: LocalDate,
)