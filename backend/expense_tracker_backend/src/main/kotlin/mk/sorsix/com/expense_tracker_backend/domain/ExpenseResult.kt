package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseResponse

sealed class CreateExpenseResult {
    data class Success(val expense: ExpenseResponse) : CreateExpenseResult()
    object CategoryNotFound : CreateExpenseResult()
}

sealed class UpdateExpenseResult {
    data class Success(val expense: ExpenseResponse) : UpdateExpenseResult()
    object ExpenseNotFound : UpdateExpenseResult()
    object CategoryNotFound : UpdateExpenseResult()
    object NotOwner : UpdateExpenseResult()
}

sealed class DeleteExpenseResult {
    object Success : DeleteExpenseResult()
    object ExpenseNotFound : DeleteExpenseResult()
    object NotOwner : DeleteExpenseResult()
}