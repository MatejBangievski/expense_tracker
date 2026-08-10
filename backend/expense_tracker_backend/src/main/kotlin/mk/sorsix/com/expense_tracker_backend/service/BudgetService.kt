package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Budget
import mk.sorsix.com.expense_tracker_backend.domain.CreateBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.BudgetResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreateBudgetRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateBudgetRequest
import mk.sorsix.com.expense_tracker_backend.repository.BudgetRepository
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySavingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val categoryService: CategoryService,
    private val monthlySavingService: MonthlySavingService,
    private val expenseRepository: ExpenseRepository
) {
    fun listBudgetsByMonth(user: User, month: LocalDate): List<BudgetResponse> {
        val monthlySaving = monthlySavingService.findByUserAndMonth(user.id, month)
            ?: return emptyList()
        val budgets = budgetRepository.findByMonthlySavingId(monthlySaving.id)
        val refreshed = budgets.map { refreshActualSpent(it, month) }
        return refreshed.map { it.toResponse(month) }
    }

    fun createBudget(user: User, request: CreateBudgetRequest): CreateBudgetResult {
        val foundCategory = categoryService.findCategoryById(request.categoryId)
            ?: return CreateBudgetResult.CategoryNotFound

        val categoryOwner = foundCategory.user
        if (categoryOwner != null && categoryOwner.id != user.id) {
            return CreateBudgetResult.CategoryNotFound
        }

        val monthlySaving = monthlySavingService.getOrCreate(user, request.budgetMonth)

        val existing = budgetRepository.findByMonthlySavingIdAndCategoryId(monthlySaving.id, request.categoryId)
        if (existing != null) {
            return CreateBudgetResult.AlreadyExists
        }

        val saved = budgetRepository.save(
            Budget(
                user = user,
                category = foundCategory,
                monthlySaving = monthlySaving,
                monthlyLimit = request.monthlyLimit,
                reason = request.reason
            )
        )

        return CreateBudgetResult.Success(saved.toResponse(request.budgetMonth))
    }

    fun updateBudget(user: User, budgetId: Long, request: UpdateBudgetRequest): UpdateBudgetResult {
        val existing = budgetRepository.findById(budgetId).orElse(null)
            ?: return UpdateBudgetResult.BudgetNotFound

        if (existing.user.id != user.id) {
            return UpdateBudgetResult.NotOwner
        }

        val updated = budgetRepository.save(
            existing.copy(monthlyLimit = request.monthlyLimit, reason = request.reason)
        )

        return UpdateBudgetResult.Success(updated.toResponse(existing.monthlySaving.savingMonth))
    }

    fun deleteBudget(user: User, budgetId: Long): DeleteBudgetResult {
        val existing = budgetRepository.findById(budgetId).orElse(null)
            ?: return DeleteBudgetResult.BudgetNotFound

        if (existing.user.id != user.id) {
            return DeleteBudgetResult.NotOwner
        }

        budgetRepository.delete(existing)
        return DeleteBudgetResult.Success
    }

    private fun refreshActualSpent(budget: Budget, month: LocalDate): Budget {
        val start = month.withDayOfMonth(1)
        val end = month.withDayOfMonth(month.lengthOfMonth())

        val spent = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
            budget.user.id, budget.category.id, start, end
        )

        return budgetRepository.save(budget.copy(actualSpent = spent))
    }

    private fun Budget.toResponse(month: LocalDate) = BudgetResponse(
        id = id,
        categoryId = category.id,
        categoryName = category.name,
        budgetMonth = month,
        monthlyLimit = monthlyLimit,
        actualSpent = actualSpent,
        reason = reason
    )
}