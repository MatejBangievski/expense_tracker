package mk.sorsix.com.expense_tracker_backend.service


import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.CreateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.Expense
import mk.sorsix.com.expense_tracker_backend.domain.FindExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.RecentExpensesResult
import mk.sorsix.com.expense_tracker_backend.domain.TopCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.TotalSpentResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseFilter
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.TopCategoryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.TotalResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseSpecifications
import mk.sorsix.com.expense_tracker_backend.util.money
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val categoryService: CategoryService,
    private val planService: PlanService,
    private val monthlySavingService: MonthlySavingService,
    private val clock: Clock,
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

    fun recentExpenses(user: User, limit: Int): RecentExpensesResult {
        if (limit < 1) {
            return RecentExpensesResult.InvalidLimit
        }
        val pageable = PageRequest.of(
            0,
            limit.coerceAtMost(50),
            Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id"))
        )
        return RecentExpensesResult.Success(expenseRepository.findByUserId(user.id, pageable).map { it.toResponse() })
    }

    fun totalSpent(user: User, start: LocalDate, end: LocalDate): TotalSpentResult {
        if (end.isBefore(start)) {
            return TotalSpentResult.InvalidRange
        }
        return TotalSpentResult.Success(TotalResponse(expenseRepository.sumByUserAndDateRange(user.id, start, end)))
    }

    fun topCategory(user: User, start: LocalDate, end: LocalDate): TopCategoryResult {
        if (end.isBefore(start)) {
            return TopCategoryResult.InvalidRange
        }
        val top = expenseRepository.topCategories(user.id, start, end, PageRequest.of(0, 1)).firstOrNull()
            ?: return TopCategoryResult.NoData
        val overall = expenseRepository.sumByUserAndDateRange(user.id, start, end)
        val share = if (overall > BigDecimal.ZERO) top.totalSpent.toDouble() / overall.toDouble() else 0.0
        return TopCategoryResult.Success(TopCategoryResponse(top.categoryId, top.categoryName, top.totalSpent, share))
    }

    @Transactional
    fun createExpense(user: User, request: CreateExpenseRequest): CreateExpenseResult {
        val foundCategory = categoryService.findCategoryById(request.categoryId)
            ?: return CreateExpenseResult.CategoryNotFound

        val categoryOwner = foundCategory.user
        if (categoryOwner != null && categoryOwner.id != user.id) {
            return CreateExpenseResult.CategoryNotFound
        }

        if (request.confirmOverBudget != true) {
            overBudgetWarning(user, foundCategory, request.amount, request.expenseDate, null)?.let {
                return CreateExpenseResult.OverBudgetWarning(it)
            }
        }

        val plan = request.planId?.let { id ->
            val foundPlan = planService.findPlanById(id)
                ?: return CreateExpenseResult.PlanNotFound
            if (foundPlan.user.id != user.id) {
                return CreateExpenseResult.PlanNotFound
            }
            foundPlan
        }

        val saved = expenseRepository.save(
            Expense(
                user = user,
                category = foundCategory,
                plan = plan,
                amount = request.amount,
                expenseDate = request.expenseDate,
                description = request.description
            )
        )

        return CreateExpenseResult.Success(saved.toResponse())
    }

    @Transactional
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

        if (request.confirmOverBudget != true) {
            overBudgetWarning(user, foundCategory, request.amount, request.expenseDate, expenseId)?.let {
                return UpdateExpenseResult.OverBudgetWarning(it)
            }
        }

        val plan = request.planId?.let { id ->
            val foundPlan = planService.findPlanById(id)
                ?: return UpdateExpenseResult.PlanNotFound
            if (foundPlan.user.id != user.id) {
                return UpdateExpenseResult.PlanNotFound
            }
            foundPlan
        }

        val updated = expenseRepository.save(
            existing.copy(
                category = foundCategory,
                amount = request.amount,
                plan = plan,
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

    private fun overBudgetWarning(user: User, category: Category, amount: BigDecimal, date: LocalDate, excludeExpenseId: Long?, ): String? {
        val monthStart = LocalDate.now(clock).withDayOfMonth(1)
        if (date.withDayOfMonth(1) != monthStart) return null
        val plan = monthlySavingService.getCurrentPlan(user) ?: return null
        val monthEnd = monthStart.plusMonths(1).minusDays(1)

        val monthExpenses = expenseRepository.findByUserIdAndExpenseDateBetween(user.id, monthStart, monthEnd)
            .filter { it.id != excludeExpenseId }
        val categoriesById = categoryService.categoriesById(user.id)
        val rootId = categoryService.rootOf(category, categoriesById).id

        val categoryLimit = plan.categoryLimits.firstOrNull { it.categoryId == rootId }
        if (categoryLimit != null) {
            val categorySpent = monthExpenses
                .filter { categoryService.rootOf(it.category, categoriesById).id == rootId }
                .fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
            val overBy = categorySpent + amount - categoryLimit.monthlyLimit
            if (overBy > BigDecimal.ZERO) {
                return "This puts the ${categoryLimit.categoryName} category over its ${categoryLimit.monthlyLimit.money()} limit for this month. Add it anyway?"
            }
        }

        if (plan.totalBudgetLimit > BigDecimal.ZERO) {
            val totalSpent = monthExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
            val overBy = totalSpent + amount - plan.totalBudgetLimit
            if (overBy > BigDecimal.ZERO) {
                return "This puts you over your ${plan.totalBudgetLimit.money()} monthly budget. Add it anyway?"
            }
        }
        return null
    }

    private fun Expense.toResponse() = ExpenseResponse(
        id = id,
        categoryId = category.id,
        categoryName = category.name,
        planId = plan?.id,
        amount = amount,
        expenseDate = expenseDate,
        description = description
    )
}