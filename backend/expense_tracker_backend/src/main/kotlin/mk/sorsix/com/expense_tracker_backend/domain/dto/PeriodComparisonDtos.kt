package mk.sorsix.com.expense_tracker_backend.domain.dto

import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import java.math.BigDecimal
import java.time.LocalDate

data class ComparePeriodsRequest(
    val currentPeriodType: PeriodType,
    val currentDate: LocalDate,
    val previousPeriodType: PeriodType,
    val previousDate: LocalDate,
)

data class PeriodComparisonResponse(
    val id: Long,
    val userId: Long,
    val periodType: PeriodType,
    val currentPeriodStart: LocalDate,
    val previousPeriodStart: LocalDate,
    val currentTotalSpent: BigDecimal,
    val previousTotalSpent: BigDecimal,
    val comparisonMessage: String?,
    val categories: List<CategoryComparison> = emptyList(),
)

data class CategoryComparison(
    val categoryName: String,
    val currentAmount: BigDecimal,
    val previousAmount: BigDecimal,
)