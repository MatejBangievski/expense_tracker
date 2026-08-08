package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySavingPlanResponse

sealed class GenerateMonthlySavingPlanResult {
    data class Success(val savingPlan: MonthlySavingPlanResponse) : GenerateMonthlySavingPlanResult()
    object UserNotFound : GenerateMonthlySavingPlanResult()
    data class InsufficientData(val message: String) : GenerateMonthlySavingPlanResult()
    object AiUnavailable : GenerateMonthlySavingPlanResult()
}