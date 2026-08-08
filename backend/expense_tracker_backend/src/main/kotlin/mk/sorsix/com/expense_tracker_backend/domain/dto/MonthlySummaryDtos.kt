package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate


data class MonthlySummaryResponse(
    val summaryId: Long,
    val userId: Long,
    val summaryMonth: LocalDate,
    val totalIncome: BigDecimal?,
    val totalSpent: BigDecimal,
    val categories: List<CategorySummaryResponse> = emptyList(),
)

data class CategorySummaryResponse(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
    val subcategories: List<SubCategorySummaryResponse> = emptyList(),
)

data class SubCategorySummaryResponse(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
)

data class MonthlySummaryRunReport(
    val month: LocalDate,
    val usersProcessed: Int,
    val succeeded: Int,
    val failed: Int,
    val failedUserIds: List<Long>,
    val durationMillis: Long,
)