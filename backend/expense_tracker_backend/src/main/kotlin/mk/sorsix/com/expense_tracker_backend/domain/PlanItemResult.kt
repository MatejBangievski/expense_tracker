package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.PlanItemResponse

sealed class CreatePlanItemResult {
    data class Success(val item: PlanItemResponse) : CreatePlanItemResult()
    object PlanNotFound : CreatePlanItemResult()
    object NotOwner : CreatePlanItemResult()
    object CategoryNotFound : CreatePlanItemResult()
}

sealed class UpdatePlanItemResult {
    data class Success(val item: PlanItemResponse) : UpdatePlanItemResult()
    object ItemNotFound : UpdatePlanItemResult()
    object NotOwner : UpdatePlanItemResult()
    object CategoryNotFound : UpdatePlanItemResult()
}

sealed class DeletePlanItemResult {
    object Success : DeletePlanItemResult()
    object ItemNotFound : DeletePlanItemResult()
    object NotOwner : DeletePlanItemResult()
}