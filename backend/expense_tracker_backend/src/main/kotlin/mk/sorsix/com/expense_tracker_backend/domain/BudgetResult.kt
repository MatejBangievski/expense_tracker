package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.BudgetResponse

sealed class CreateBudgetResult {
    data class Success(
        val budget: BudgetResponse,
    ) : CreateBudgetResult()

    object CategoryNotFound : CreateBudgetResult()

    object AlreadyExists : CreateBudgetResult()
}

sealed class UpdateBudgetResult {
    data class Success(
        val budget: BudgetResponse,
    ) : UpdateBudgetResult()

    object BudgetNotFound : UpdateBudgetResult()

    object NotOwner : UpdateBudgetResult()
}

sealed class DeleteBudgetResult {
    object Success : DeleteBudgetResult()

    object BudgetNotFound : DeleteBudgetResult()

    object NotOwner : DeleteBudgetResult()
}
