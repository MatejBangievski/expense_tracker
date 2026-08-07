package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.api.GeminiApiResult
import mk.sorsix.com.expense_tracker_backend.domain.Budget
import mk.sorsix.com.expense_tracker_backend.domain.MonthlySaving
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSpendingSummary
import mk.sorsix.com.expense_tracker_backend.repository.BudgetRepository
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySavingRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class MonthlySavingService(
    private val chatClient: ChatClient,
    private val periodSummaryService: PeriodSummaryService,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val monthlySavingRepository: MonthlySavingRepository,
    private val userRepository: UserRepository,
) {
    fun findByUserAndMonth(userId: Long, month: LocalDate): MonthlySaving? =
        monthlySavingRepository.findByUserIdAndSavingMonth(userId, month)
    fun getOrCreate(user: User, month: LocalDate): MonthlySaving {
        return monthlySavingRepository.findByUserIdAndSavingMonth(user.id, month)
            ?: monthlySavingRepository.save(
                MonthlySaving(user = user, savingMonth = month, totalIncome = user.monthlySalary)
            )
    }

    @Transactional
    fun generateWithAIAndPersist(userId: Long, nextPeriodBudgetLimit: BigDecimal, totalIncome: BigDecimal? = null): GeminiApiResult {
        val periodStart: LocalDate = LocalDate.now().withDayOfMonth(1)
        val periodEnd: LocalDate = LocalDate.now()

        val user = userRepository.getReferenceById(userId)
        val savingMonth = periodStart.plusMonths(1).withDayOfMonth(1)
        val summary = periodSummaryService.summarize(userId, periodStart, periodEnd, totalIncome)

        val result = chatClient.prompt()
            .user(buildPrompt(summary, savingMonth, nextPeriodBudgetLimit))
            .call()
            .entity(GeminiApiResult::class.java)
            ?: throw IllegalStateException("Gemini returned no result")

        val totalSaved = summary.totalSpent.subtract(nextPeriodBudgetLimit).max(BigDecimal.ZERO)

        val existingSaving = monthlySavingRepository.findByUserIdAndSavingMonth(userId, savingMonth)
        val savedSaving = monthlySavingRepository.save(
            (existingSaving ?: MonthlySaving(user = user, savingMonth = savingMonth)).copy(
                totalBudgetLimit = nextPeriodBudgetLimit,
                totalIncome = totalIncome,
                totalSpent = summary.totalSpent,
                totalSaved = totalSaved,
                recommendationMessage = result.recommendationMessage,
            )
        )

        val limitsByCategory = result.monthlyPlan?.categoryLimits?.associateBy { it.categoryName } ?: emptyMap()
        val actualsByCategory = summary.categories.associateBy { it.categoryName }
        val allCategoryNames = limitsByCategory.keys + actualsByCategory.keys

        allCategoryNames.forEach { categoryName ->
            val category = categoryRepository.findByNameIgnoreCase(categoryName)
            if (category != null) {
                val existingBudget = budgetRepository.findByMonthlySavingIdAndCategoryId(savedSaving.id, category.id)
                val limit = limitsByCategory[categoryName]
                val actual = actualsByCategory[categoryName]
                val budget = (existingBudget ?: Budget(user = user, category = category, monthlySaving = savedSaving)).copy(
                    monthlyLimit = limit?.suggestedLimit ?: existingBudget?.monthlyLimit ?: BigDecimal.ZERO,
                    actualSpent = actual?.totalAmount ?: existingBudget?.actualSpent,
                    reason = limit?.reason ?: existingBudget?.reason,
                )
                budgetRepository.save(budget)
            }
        }

        return result
    }

    private fun buildPrompt(
        summary: PeriodSpendingSummary,
        savingMonth: LocalDate,
        nextPeriodBudgetLimit: BigDecimal,
    ): String = buildString {
        append(summary)
        appendLine()
        appendLine("Total budget limit to distribute across categories for $savingMonth: $nextPeriodBudgetLimit")
    }
}