package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate


data class MonthlySummaryView(
    val summaryId: Long,
    val userId: Long,
    val summaryMonth: LocalDate,
    val totalIncome: BigDecimal?,
    val totalSpent: BigDecimal,
    val categories: List<CategoryRollup> = emptyList(),
)

data class CategoryRollup(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
    val subcategories: List<SubCategoryRollup> = emptyList(),
)

data class SubCategoryRollup(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
)
