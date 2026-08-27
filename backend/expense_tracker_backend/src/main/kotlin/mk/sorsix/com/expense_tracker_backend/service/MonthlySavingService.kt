package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.config.AiPrompts
import mk.sorsix.com.expense_tracker_backend.config.UserChatClientProvider
import mk.sorsix.com.expense_tracker_backend.domain.Budget
import mk.sorsix.com.expense_tracker_backend.domain.CategoryBudgetSuggestion
import mk.sorsix.com.expense_tracker_backend.domain.FindPeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySavingPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.MonthlySaving
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryLimitResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.ManualSavingPlanRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySavingPlanResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.BudgetRepository
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySavingRepository
import mk.sorsix.com.expense_tracker_backend.util.money
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

@Service
class MonthlySavingService(
    private val userChatClientProvider: UserChatClientProvider,
    private val aiPrompts: AiPrompts,
    private val periodSummaryService: PeriodSummaryService,
    private val monthlySavingRepository: MonthlySavingRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun findByUserAndMonth(userId: Long, month: LocalDate): MonthlySaving? =
        monthlySavingRepository.findByUserIdAndSavingMonth(userId, month)

    fun getOrCreate(user: User, month: LocalDate): MonthlySaving =
        monthlySavingRepository.findByUserIdAndSavingMonth(user.id, month)
            ?: monthlySavingRepository.save(
                MonthlySaving(user = user, savingMonth = month, totalIncome = user.monthlySalary)
            )

    @Transactional
    fun generateWithAIAndPersist(user: User, budgetLimit: BigDecimal, totalIncome: BigDecimal? = null): GenerateMonthlySavingPlanResult {
        val savingMonth = LocalDate.now(clock).withDayOfMonth(1)

        val currentSummary = periodSummaryService.currentMonthSpending(user.id)
        val previousSummary = when (val r = periodSummaryService.find(user.id, PeriodType.MONTH, savingMonth.minusMonths(1))) {
            is FindPeriodSummaryResult.Success -> r.summary
            is FindPeriodSummaryResult.SummaryNotFound -> null
        }

        val aiResult = try {
            userChatClientProvider.forUser(user).prompt()
                .system(aiPrompts.monthlySavingRecommendation)
                .user(buildPrompt(previousSummary, currentSummary, savingMonth, budgetLimit))
                .call()
                .entity(GeminiApiResult::class.java)
        } catch (ex: Exception) {
            log.error("Gemini call failed for userId={} month={}", user.id, savingMonth, ex)
            null
        } ?: return GenerateMonthlySavingPlanResult.AiUnavailable

        if (!aiResult.success) {
            return GenerateMonthlySavingPlanResult.InsufficientData(aiResult.recommendationMessage)
        }

        return GenerateMonthlySavingPlanResult.Success(
            persist(
                user, savingMonth, budgetLimit, totalIncome, currentSummary,
                aiResult.recommendationMessage, aiResult.monthlyPlan?.categoryLimits ?: emptyList(),
            ),
        )
    }

    @Transactional
    fun createManualPlan(user: User, request: ManualSavingPlanRequest): GenerateMonthlySavingPlanResult {
        val savingMonth = LocalDate.now(clock).withDayOfMonth(1)
        val summary = periodSummaryService.currentMonthSpending(user.id)
        return GenerateMonthlySavingPlanResult.Success(
            persist(
                user, savingMonth, request.budgetLimit, request.totalIncome, summary,
                request.recommendationMessage, request.categoryLimits,
            ),
        )
    }

    fun getCurrentPlan(user: User): MonthlySavingPlanResponse? {
        val savingMonth = LocalDate.now(clock).withDayOfMonth(1)
        val saving = monthlySavingRepository.findByUserIdAndSavingMonth(user.id, savingMonth) ?: return null
        val plan = saving.toResponse(budgetRepository.findByMonthlySavingId(saving.id))

        val live = periodSummaryService.currentMonthSpending(user.id)
        val spentByName = live.categories.associate { it.categoryName.lowercase() to it.totalAmount }
        return plan.copy(
            totalSpent = live.totalSpent,
            totalSaved = plan.totalBudgetLimit.subtract(live.totalSpent),
            categoryLimits = plan.categoryLimits.map {
                it.copy(actualSpent = spentByName[it.categoryName.lowercase()] ?: BigDecimal.ZERO)
            },
        )
    }

    private fun persist(user: User, savingMonth: LocalDate, budgetLimit: BigDecimal, totalIncome: BigDecimal?, summary: PeriodSummaryResponse, recommendationMessage: String?, categoryLimits: List<CategoryBudgetSuggestion>): MonthlySavingPlanResponse {
        val totalBudgetLimit = budgetLimit.max(summary.totalSpent)
        val totalSaved = totalBudgetLimit.subtract(summary.totalSpent)

        val existingSaving = monthlySavingRepository.findByUserIdAndSavingMonth(user.id, savingMonth)
        val savedSaving = monthlySavingRepository.save(
            (existingSaving ?: MonthlySaving(user = user, savingMonth = savingMonth)).copy(
                totalBudgetLimit = totalBudgetLimit,
                totalIncome = totalIncome,
                totalSpent = summary.totalSpent,
                totalSaved = totalSaved,
                recommendationMessage = recommendationMessage,
            )
        )

        val limitsByName = categoryLimits.associateBy { it.categoryName.lowercase() }
        val actualsByName = summary.categories.associateBy { it.categoryName.lowercase() }
        val allNames = limitsByName.keys + actualsByName.keys
        val categoriesByName = categoryRepository.findByNameIgnoreCaseIn(allNames).associateBy { it.name.lowercase() }
        val existingBudgetsByCategoryId = budgetRepository.findByMonthlySavingId(savedSaving.id)
            .associateBy { it.category.id }

        val budgets = allNames.mapNotNull { name ->
            val category = categoriesByName[name] ?: return@mapNotNull null
            val existingBudget = existingBudgetsByCategoryId[category.id]
            val limit = limitsByName[name]
            val actual = actualsByName[name]
            val actualSpent = actual?.totalAmount ?: existingBudget?.actualSpent
            val requestedLimit = limit?.suggestedLimit ?: existingBudget?.monthlyLimit ?: BigDecimal.ZERO
            budgetRepository.save(
                (existingBudget ?: Budget(user = user, category = category, monthlySaving = savedSaving)).copy(
                    monthlyLimit = requestedLimit.max(actualSpent ?: BigDecimal.ZERO),
                    actualSpent = actualSpent,
                    reason = limit?.reason ?: existingBudget?.reason,
                )
            )
        }

        return savedSaving.toResponse(budgets)
    }

    private fun MonthlySaving.toResponse(budgets: List<Budget>) = MonthlySavingPlanResponse(
        id = id,
        savingMonth = savingMonth,
        totalBudgetLimit = totalBudgetLimit,
        totalIncome = totalIncome,
        totalSpent = totalSpent,
        totalSaved = totalSaved,
        recommendationMessage = recommendationMessage,
        categoryLimits = budgets
            .map { CategoryLimitResponse(it.category.id, it.category.name, it.monthlyLimit, it.actualSpent, it.reason) }
            .sortedByDescending { it.monthlyLimit },
    )

    private fun buildPrompt(previousSummary: PeriodSummaryResponse?, currentSummary: PeriodSummaryResponse, savingMonth: LocalDate, budgetLimit: BigDecimal, ): String = buildString {
        appendLine("You are planning the budget for the CURRENT month: $savingMonth")
        appendLine()

        if (previousSummary != null) {
            appendLine("=== PREVIOUS MONTH (${previousSummary.periodStart}) — the basis for your advice ===")
            previousSummary.totalIncome?.let { appendLine("Reported monthly income: ${it.money()}") }
            appendLine("Total spent: ${previousSummary.totalSpent.money()}")
            appendSpendingByCategory(previousSummary)
        } else {
            appendLine("No previous month is available for reference (this may be the user's first month).")
        }
        appendLine()

        appendLine("=== CURRENT MONTH SO FAR (${currentSummary.periodStart}) — already spent, NEVER allocate below these ===")
        currentSummary.totalIncome?.let { appendLine("Reported monthly income: ${it.money()}") }
        appendLine("Total already spent this month: ${currentSummary.totalSpent.money()}")
        appendSpendingByCategory(currentSummary)
        appendLine()

        appendLine("The user has fixed a total budget of ${budgetLimit.money()} for this month. Distribute it across the categories; do not invent or override it.")
        appendLine("Every category limit must be at least what was already spent in that category this month.")
    }

    private fun StringBuilder.appendSpendingByCategory(summary: PeriodSummaryResponse) {
        if (summary.categories.isEmpty()) {
            appendLine("No spending was recorded.")
            return
        }
        appendLine("Spending by category (top-level category, then its subcategories):")
        summary.categories.forEach { category ->
            appendLine("- ${category.categoryName}: ${category.totalAmount.money()} across ${category.expenseCount} expense(s)")
            category.subcategories.forEach { sub ->
                val label = if (sub.categoryId == category.categoryId) {
                    "${sub.categoryName} (spent directly on this category)"
                } else {
                    sub.categoryName
                }
                appendLine("    - $label: ${sub.totalAmount.money()} across ${sub.expenseCount} expense(s)")
            }
        }
    }
}