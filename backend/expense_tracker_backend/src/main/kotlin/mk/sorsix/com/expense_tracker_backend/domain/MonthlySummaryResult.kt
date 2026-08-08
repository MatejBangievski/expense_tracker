package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySummaryResponse

sealed class GenerateMonthlySummaryResult {
    data class Success(val summary: MonthlySummaryResponse) : GenerateMonthlySummaryResult()
    object UserNotFound : GenerateMonthlySummaryResult()
}

sealed class FindMonthlySummaryResult {
    data class Success(val summary: MonthlySummaryResponse) : FindMonthlySummaryResult()
    object SummaryNotFound : FindMonthlySummaryResult()
}