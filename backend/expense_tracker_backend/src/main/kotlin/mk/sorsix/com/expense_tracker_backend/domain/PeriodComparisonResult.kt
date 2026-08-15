package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodComparisonResponse

sealed class GeneratePeriodComparisonResult {
    data class Success(val comparison: PeriodComparisonResponse) : GeneratePeriodComparisonResult()
    object SamePeriod : GeneratePeriodComparisonResult()
    object InadequatePeriods : GeneratePeriodComparisonResult()
}