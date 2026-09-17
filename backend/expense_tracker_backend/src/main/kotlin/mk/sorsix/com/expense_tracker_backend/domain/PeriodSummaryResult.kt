package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.AvailablePeriod
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse

sealed class GeneratePeriodSummaryResult {
    data class Success(
        val summary: PeriodSummaryResponse,
    ) : GeneratePeriodSummaryResult()

    object UserNotFound : GeneratePeriodSummaryResult()
}

sealed class FindPeriodSummaryResult {
    data class Success(
        val summary: PeriodSummaryResponse,
    ) : FindPeriodSummaryResult()

    object SummaryNotFound : FindPeriodSummaryResult()
}

sealed class CurrentSpendingResult {
    data class Success(
        val summary: PeriodSummaryResponse,
    ) : CurrentSpendingResult()

    object UserNotFound : CurrentSpendingResult()
}

sealed class AvailablePeriodsResult {
    data class Success(
        val periods: List<AvailablePeriod>,
    ) : AvailablePeriodsResult()

    object UserNotFound : AvailablePeriodsResult()
}
