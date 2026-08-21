package mk.sorsix.com.expense_tracker_backend.service


import mk.sorsix.com.expense_tracker_backend.domain.CreateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.Expense
import mk.sorsix.com.expense_tracker_backend.domain.FindExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseFilter
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseSpecifications
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val categoryService: CategoryService
) {

    fun findExpenseById(expenseId: Long): Expense? = expenseRepository.findByIdOrNull(expenseId)

    fun findById(user: User, id: Long): FindExpenseResult {
        val expense = findExpenseById(id)
            ?: return FindExpenseResult.ExpenseNotFound

        if (expense.user.id != user.id) {
            return FindExpenseResult.NotOwner
        }

        return FindExpenseResult.Success(expense.toResponse())
    }

    fun listExpenses(user: User, filter: ExpenseFilter): List<ExpenseResponse> {
        var specification = ExpenseSpecifications.belongsToUser(user.id)
        ExpenseSpecifications.categoryNameEquals(filter.categoryName)?.let { specification = specification.and(it) }
        ExpenseSpecifications.betweenDates(filter.periodStart, filter.periodEnd)
            ?.let { specification = specification.and(it) }
        ExpenseSpecifications.descriptionContains(filter.search)?.let { specification = specification.and(it) }
        return expenseRepository.findAll(specification).map {
            it.toResponse()
        }
    }

    fun createExpense(user: User, request: CreateExpenseRequest): CreateExpenseResult {
        val foundCategory = categoryService.findCategoryById(request.categoryId)
            ?: return CreateExpenseResult.CategoryNotFound

        val categoryOwner = foundCategory.user
        if (categoryOwner != null && categoryOwner.id != user.id) {
            return CreateExpenseResult.CategoryNotFound
        }

        val saved = expenseRepository.save(
            Expense(
                user = user,
                category = foundCategory,
                amount = request.amount,
                expenseDate = request.expenseDate,
                description = request.description
            )
        )

        return CreateExpenseResult.Success(saved.toResponse())
    }

    fun updateExpense(user: User, expenseId: Long, request: UpdateExpenseRequest): UpdateExpenseResult {
        val existing = expenseRepository.findById(expenseId).orElse(null)
            ?: return UpdateExpenseResult.ExpenseNotFound

        if (existing.user.id != user.id) {
            return UpdateExpenseResult.NotOwner
        }

        if (!existing.active) {
            return UpdateExpenseResult.ExpenseLocked
        }

        val foundCategory = categoryService.findCategoryById(request.categoryId)
            ?: return UpdateExpenseResult.CategoryNotFound

        val categoryOwner = foundCategory.user
        if (categoryOwner != null && categoryOwner.id != user.id) {
            return UpdateExpenseResult.CategoryNotFound
        }

        val updated = expenseRepository.save(
            existing.copy(
                category = foundCategory,
                amount = request.amount,
                expenseDate = request.expenseDate,
                description = request.description
            )
        )

        return UpdateExpenseResult.Success(updated.toResponse())
    }

    fun deleteExpense(user: User, expenseId: Long): DeleteExpenseResult {
        val existing = findExpenseById(expenseId)
            ?: return DeleteExpenseResult.ExpenseNotFound

        if (existing.user.id != user.id) {
            return DeleteExpenseResult.NotOwner
        }

        if (!existing.active) {
            return DeleteExpenseResult.ExpenseLocked
        }

        expenseRepository.delete(existing)
        return DeleteExpenseResult.Success
    }

    private fun Expense.toResponse() = ExpenseResponse(
        id = id,
        categoryId = category.id,
        categoryName = category.name,
        amount = amount,
        expenseDate = expenseDate,
        description = description
    )
}