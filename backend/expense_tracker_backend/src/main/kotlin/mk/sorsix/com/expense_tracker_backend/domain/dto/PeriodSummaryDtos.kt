package mk.sorsix.com.expense_tracker_backend.domain.dto

import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import java.math.BigDecimal
import java.time.LocalDate

data class PeriodSummaryResponse(
    val summaryId: Long,
    val userId: Long,
    val periodType: PeriodType,
    val periodStart: LocalDate,
    val totalIncome: BigDecimal?,
    val totalSpent: BigDecimal,
    val categories: List<CategorySummaryResponse> = emptyList(),
)

data class AvailablePeriod(
    val periodStart: LocalDate,
    val totalSpent: BigDecimal,
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

data class PeriodSummaryRunReport(
    val periodType: PeriodType,
    val periodStart: LocalDate,
    val usersProcessed: Int,
    val succeeded: Int,
    val failed: Int,
    val failedUserIds: List<Long>,
    val durationMillis: Long,
)
