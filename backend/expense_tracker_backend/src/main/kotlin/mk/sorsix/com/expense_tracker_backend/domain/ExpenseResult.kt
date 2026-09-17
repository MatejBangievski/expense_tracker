package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.TopCategoryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.TotalResponse

sealed class CreateExpenseResult {
    data class Success(
        val expense: ExpenseResponse,
    ) : CreateExpenseResult()

    object CategoryNotFound : CreateExpenseResult()

    object PlanNotFound : CreateExpenseResult()

    data class OverBudgetWarning(
        val message: String,
    ) : CreateExpenseResult()
}

sealed class UpdateExpenseResult {
    data class Success(
        val expense: ExpenseResponse,
    ) : UpdateExpenseResult()

    object ExpenseNotFound : UpdateExpenseResult()

    object CategoryNotFound : UpdateExpenseResult()

    object NotOwner : UpdateExpenseResult()

    object ExpenseLocked : UpdateExpenseResult()

    object PlanNotFound : UpdateExpenseResult()

    data class OverBudgetWarning(
        val message: String,
    ) : UpdateExpenseResult()
}

sealed class DeleteExpenseResult {
    object Success : DeleteExpenseResult()

    object ExpenseNotFound : DeleteExpenseResult()

    object NotOwner : DeleteExpenseResult()

    object ExpenseLocked : DeleteExpenseResult()
}

sealed class FindExpenseResult {
    data class Success(
        val expense: ExpenseResponse,
    ) : FindExpenseResult()

    object ExpenseNotFound : FindExpenseResult()

    object NotOwner : FindExpenseResult()
}

sealed class RecentExpensesResult {
    data class Success(
        val expenses: List<ExpenseResponse>,
    ) : RecentExpensesResult()

    object InvalidLimit : RecentExpensesResult()
}

sealed class TotalSpentResult {
    data class Success(
        val total: TotalResponse,
    ) : TotalSpentResult()

    object InvalidRange : TotalSpentResult()
}

sealed class TopCategoryResult {
    data class Success(
        val topCategory: TopCategoryResponse,
    ) : TopCategoryResult()

    object NoData : TopCategoryResult()

    object InvalidRange : TopCategoryResult()
}
