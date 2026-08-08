package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.PlanResponse

sealed class CreatePlanResult {
    data class Success(val plan: PlanResponse) : CreatePlanResult()
    object InvalidDateRange : CreatePlanResult()
}

sealed class UpdatePlanResult {
    data class Success(val plan: PlanResponse) : UpdatePlanResult()
    object PlanNotFound : UpdatePlanResult()
    object NotOwner : UpdatePlanResult()
    object InvalidDateRange : UpdatePlanResult()
}

sealed class DeletePlanResult {
    object Success : DeletePlanResult()
    object PlanNotFound : DeletePlanResult()
    object NotOwner : DeletePlanResult()
}