package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.config.AiPrompts
import mk.sorsix.com.expense_tracker_backend.config.UserChatClientProvider
import mk.sorsix.com.expense_tracker_backend.domain.Budget
import mk.sorsix.com.expense_tracker_backend.domain.CategoryBudgetSuggestion
import mk.sorsix.com.expense_tracker_backend.domain.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySavingPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate

@Service
class MonthlySavingService(
    private val userChatClientProvider: UserChatClientProvider,
    private val aiPrompts: AiPrompts,
    private val periodSummaryService: PeriodSummaryService,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val monthlySavingRepository: MonthlySavingRepository,
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
    fun generateWithAIAndPersist(user: User, nextPeriodBudgetLimit: BigDecimal, totalIncome: BigDecimal? = null): GenerateMonthlySavingPlanResult {
        val currentMonth = LocalDate.now(clock).withDayOfMonth(1)
        val savingMonth = currentMonth.plusMonths(1)

        val summary = generateMonthSummary(user, currentMonth, totalIncome)

        val aiResult = try {
            userChatClientProvider.forUser(user).prompt()
                .system(aiPrompts.monthlySavingRecommendation)
                .user(buildPrompt(summary, savingMonth, nextPeriodBudgetLimit))
                .call()
                .entity(GeminiApiResult::class.java)
        } catch (ex: Exception) {
            log.error("Gemini call failed for userId={} month={}", user.id, savingMonth, ex)
            null
        } ?: return GenerateMonthlySavingPlanResult.AiUnavailable

        if (!aiResult.success) {
            return GenerateMonthlySavingPlanResult.InsufficientData(aiResult.recommendationMessage)
        }

        return persistPlan(user, savingMonth, nextPeriodBudgetLimit, totalIncome, summary, aiResult.recommendationMessage, aiResult.monthlyPlan?.categoryLimits ?: emptyList(),)
    }

    @Transactional
    fun createManualPlan(user: User, request: ManualSavingPlanRequest): GenerateMonthlySavingPlanResult {
        val currentMonth = LocalDate.now(clock).withDayOfMonth(1)
        val savingMonth = currentMonth.plusMonths(1)

        val summary = generateMonthSummary(user, currentMonth, request.totalIncome)

        return persistPlan(
            user, savingMonth, request.budgetLimit, request.totalIncome, summary,
            request.recommendationMessage, request.categoryLimits,
        )
    }

    private fun persistPlan(user: User, savingMonth: LocalDate, nextPeriodBudgetLimit: BigDecimal, totalIncome: BigDecimal?, summary: PeriodSummaryResponse, recommendationMessage: String?, categoryLimits: List<CategoryBudgetSuggestion>): GenerateMonthlySavingPlanResult {
        // Mozhebi nekoja bolje logika ovde
        val totalSaved = summary.totalSpent.subtract(nextPeriodBudgetLimit).max(BigDecimal.ZERO)

        val existingSaving = monthlySavingRepository.findByUserIdAndSavingMonth(user.id, savingMonth)
        val savedSaving = monthlySavingRepository.save(
            (existingSaving ?: MonthlySaving(user = user, savingMonth = savingMonth)).copy(
                totalBudgetLimit = nextPeriodBudgetLimit,
                totalIncome = totalIncome,
                totalSpent = summary.totalSpent,
                totalSaved = totalSaved,
                recommendationMessage = recommendationMessage,
            )
        )

        val limitsByCategory = categoryLimits.associateBy { it.categoryName }
        val actualsByCategory = summary.categories.associateBy { it.categoryName }

        //        val budgets = allCategoryNames.mapNotNull { categoryName ->
//            val category = categoryRepository.findByNameIgnoreCase(categoryName)
//            if (category == null) {
//                null
//            } else {
//                val existingBudget = budgetRepository.findByMonthlySavingIdAndCategoryId(savedSaving.id, category.id)
        // stara verzija proverena deka raboti
        val allCategoryNames = limitsByCategory.keys + actualsByCategory.keys
        val categoriesByName = categoryRepository.findByNameIgnoreCaseIn(allCategoryNames)
            .associateBy { it.name.lowercase() }
        val existingBudgetsByCategoryId = budgetRepository.findByMonthlySavingId(savedSaving.id)
            .associateBy { it.category.id }
        val budgets = allCategoryNames.mapNotNull { categoryName ->
            val category = categoriesByName[categoryName.lowercase()]
            if (category == null) {
                null
            } else {
                val existingBudget = existingBudgetsByCategoryId[category.id]
                val limit = limitsByCategory[categoryName]
                val actual = actualsByCategory[categoryName]
                budgetRepository.save(
                    (existingBudget ?: Budget(user = user, category = category, monthlySaving = savedSaving)).copy(
                        monthlyLimit = limit?.suggestedLimit ?: existingBudget?.monthlyLimit ?: BigDecimal.ZERO,
                        actualSpent = actual?.totalAmount ?: existingBudget?.actualSpent,
                        reason = limit?.reason ?: existingBudget?.reason,
                    )
                )
            }
        }

        return GenerateMonthlySavingPlanResult.Success(savedSaving.toResponse(budgets))
    }

    private fun generateMonthSummary(user: User, month: LocalDate, totalIncome: BigDecimal?): PeriodSummaryResponse =
        when (val result = periodSummaryService.generateForUser(user.id, PeriodType.MONTH, month, totalIncome)) {
            is GeneratePeriodSummaryResult.Success -> result.summary
            is GeneratePeriodSummaryResult.UserNotFound ->
                error("summary generation reported unknown user for an authenticated principal")
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

    private fun buildPrompt(summary: PeriodSummaryResponse, savingMonth: LocalDate, nextPeriodBudgetLimit: BigDecimal, ): String = buildString {
        appendLine("Spending summary for month: ${summary.periodStart}")
        summary.totalIncome?.let { appendLine("Reported monthly income: ${it.money()}") }
        appendLine("Total spent this month: ${summary.totalSpent.money()}")
        appendLine()

        if (summary.categories.isEmpty()) {
            appendLine("No spending was recorded for this month.")
        } else {
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

        appendLine()
        appendLine("Total budget limit to distribute across categories for $savingMonth: $nextPeriodBudgetLimit")
    }

    private fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
}