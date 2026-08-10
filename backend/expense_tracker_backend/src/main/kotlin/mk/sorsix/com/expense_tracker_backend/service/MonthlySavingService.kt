package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Budget
import mk.sorsix.com.expense_tracker_backend.domain.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySavingPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.MonthlySaving
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryLimitResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySavingPlanResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.BudgetRepository
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySavingRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate

@Service
class MonthlySavingService(
    private val chatClient: ChatClient,
    private val monthlySummaryService: MonthlySummaryService,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val monthlySavingRepository: MonthlySavingRepository,
    private val userRepository: UserRepository,
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
    fun generateWithAIAndPersist(userId: Long, nextPeriodBudgetLimit: BigDecimal, totalIncome: BigDecimal? = null): GenerateMonthlySavingPlanResult {
        val currentMonth = LocalDate.now(clock).withDayOfMonth(1)
        val savingMonth = currentMonth.plusMonths(1)

        val user = userRepository.findById(userId).orElse(null)
            ?: return GenerateMonthlySavingPlanResult.UserNotFound

        val summary = when (val result = monthlySummaryService.generateForUser(userId, currentMonth, totalIncome)) {
            is GenerateMonthlySummaryResult.Success -> result.summary
            is GenerateMonthlySummaryResult.UserNotFound -> return GenerateMonthlySavingPlanResult.UserNotFound
        }

        val aiResult = try {
            chatClient.prompt()
                .user(buildPrompt(summary, savingMonth, nextPeriodBudgetLimit))
                .call()
                .entity(GeminiApiResult::class.java)
        } catch (ex: Exception) {
            log.error("Gemini call failed for userId={} month={}", userId, savingMonth, ex)
            null
        } ?: return GenerateMonthlySavingPlanResult.AiUnavailable

        if (!aiResult.success) {
            return GenerateMonthlySavingPlanResult.InsufficientData(aiResult.recommendationMessage)
        }

        // Mozhebi nekoja bolje logika ovde
        val totalSaved = summary.totalSpent.subtract(nextPeriodBudgetLimit).max(BigDecimal.ZERO)

        val existingSaving = monthlySavingRepository.findByUserIdAndSavingMonth(userId, savingMonth)
        val savedSaving = monthlySavingRepository.save(
            (existingSaving ?: MonthlySaving(user = user, savingMonth = savingMonth)).copy(
                totalBudgetLimit = nextPeriodBudgetLimit,
                totalIncome = totalIncome,
                totalSpent = summary.totalSpent,
                totalSaved = totalSaved,
                recommendationMessage = aiResult.recommendationMessage,
            )
        )

        val limitsByCategory = aiResult.monthlyPlan?.categoryLimits?.associateBy { it.categoryName } ?: emptyMap()
        val actualsByCategory = summary.categories.associateBy { it.categoryName }
        val allCategoryNames = limitsByCategory.keys + actualsByCategory.keys

        val budgets = allCategoryNames.mapNotNull { categoryName ->
            val category = categoryRepository.findByNameIgnoreCase(categoryName)
            if (category == null) {
                null
            } else {
                val existingBudget = budgetRepository.findByMonthlySavingIdAndCategoryId(savedSaving.id, category.id)
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

    private fun buildPrompt(summary: MonthlySummaryResponse, savingMonth: LocalDate, nextPeriodBudgetLimit: BigDecimal, ): String = buildString {
        appendLine("Spending summary for month: ${summary.summaryMonth}")
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