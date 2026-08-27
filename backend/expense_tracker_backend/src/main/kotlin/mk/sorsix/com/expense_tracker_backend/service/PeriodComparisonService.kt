package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.config.AiPrompts
import mk.sorsix.com.expense_tracker_backend.config.UserChatClientProvider
import mk.sorsix.com.expense_tracker_backend.domain.FindPeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GeminiComparisonResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodComparisonResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodComparison
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryComparison
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodComparisonResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.PeriodComparisonRepository
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PeriodComparisonService(
    private val userChatClientProvider: UserChatClientProvider,
    private val aiPrompts: AiPrompts,
    private val periodSummaryService: PeriodSummaryService,
    private val periodSummaryRepository: PeriodSummaryRepository,
    private val periodComparisonRepository: PeriodComparisonRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun compareWithPreviousPeriod(user: User, periodType: PeriodType, date: LocalDate? = null, useAi: Boolean = false): GeneratePeriodComparisonResult {
        val current = periodType.startOf(date ?: LocalDate.now(clock))
        return compare(user, periodType, current, periodType, periodType.previous(current), useAi)
    }

    @Transactional
    fun compare(user: User, currentPeriodType: PeriodType, currentDate: LocalDate, previousPeriodType: PeriodType, previousDate: LocalDate, useAi: Boolean = false): GeneratePeriodComparisonResult {
        if (currentPeriodType != previousPeriodType) {
            return GeneratePeriodComparisonResult.InadequatePeriods
        }

        val periodType = currentPeriodType
        val current = periodType.startOf(currentDate)
        val previous = periodType.startOf(previousDate)

        if (current == previous) {
            return GeneratePeriodComparisonResult.SamePeriod
        }

        val currentSummary = findOrCreateSummary(user, periodType, current)
        val previousSummary = findOrCreateSummary(user, periodType, previous)

        var comparison = periodComparisonRepository
            .findByCurrentSummaryIdAndPreviousSummaryId(currentSummary.summaryId, previousSummary.summaryId)
            ?: periodComparisonRepository.save(
                PeriodComparison(
                    user = user,
                    currentSummary = periodSummaryRepository.getReferenceById(currentSummary.summaryId),
                    previousSummary = periodSummaryRepository.getReferenceById(previousSummary.summaryId),
                )
            )

        if (useAi) {
            generateMessage(user, currentSummary, previousSummary)?.let {
                comparison = periodComparisonRepository.save(comparison.copy(comparisonMessage = it))
            }
        }

        return GeneratePeriodComparisonResult.Success(
            PeriodComparisonResponse(
                id = comparison.id,
                userId = user.id,
                periodType = periodType,
                currentPeriodStart = current,
                previousPeriodStart = previous,
                currentTotalSpent = currentSummary.totalSpent,
                previousTotalSpent = previousSummary.totalSpent,
                comparisonMessage = comparison.comparisonMessage,
                categories = categoryComparisons(currentSummary, previousSummary),
            )
        )
    }

    private fun categoryComparisons(current: PeriodSummaryResponse, previous: PeriodSummaryResponse): List<CategoryComparison> {
        val currentByName = current.categories.associateBy { it.categoryName }
        val previousByName = previous.categories.associateBy { it.categoryName }
        return (currentByName.keys + previousByName.keys)
            .map { name ->
                CategoryComparison(
                    categoryName = name,
                    currentAmount = currentByName[name]?.totalAmount ?: BigDecimal.ZERO,
                    previousAmount = previousByName[name]?.totalAmount ?: BigDecimal.ZERO,
                )
            }
            .sortedByDescending { it.currentAmount }
    }

    private fun findOrCreateSummary(user: User, periodType: PeriodType, date: LocalDate): PeriodSummaryResponse =
        when (val found = periodSummaryService.find(user.id, periodType, date)) {
            is FindPeriodSummaryResult.Success -> found.summary
            is FindPeriodSummaryResult.SummaryNotFound ->
                when (val generated = periodSummaryService.generateForUser(user.id, periodType, date)) {
                    is GeneratePeriodSummaryResult.Success -> generated.summary
                    is GeneratePeriodSummaryResult.UserNotFound ->
                        error("summary generation reported unknown user for an authenticated principal")
                }
        }

    private fun generateMessage(user: User, current: PeriodSummaryResponse, previous: PeriodSummaryResponse): String? = try {
        userChatClientProvider.forUser(user).prompt()
            .system(aiPrompts.periodComparison)
            .user(buildPrompt(current, previous))
            .call()
            .entity(GeminiComparisonResult::class.java)
            ?.takeIf { it.success }
            ?.comparisonMessage
    } catch (ex: Exception) {
        log.error("Gemini comparison call failed for userId={}", user.id, ex)
        null
    }

    private fun buildPrompt(current: PeriodSummaryResponse, previous: PeriodSummaryResponse): String = buildString {
        appendLine("=== CURRENT PERIOD ===")
        appendPeriod(current)
        appendLine()
        appendLine("=== EARLIER PERIOD (compare against this) ===")
        appendPeriod(previous)
    }

    private fun StringBuilder.appendPeriod(summary: PeriodSummaryResponse) {
        val start = summary.periodStart
        val periodType = summary.periodType
        val end = periodType.endOf(start)
        val totalDays = periodType.daysIn(start)
        val isCurrentPeriod = start == periodType.startOf(LocalDate.now(clock))
        val daysElapsed = if (isCurrentPeriod) ChronoUnit.DAYS.between(start, LocalDate.now(clock)) + 1 else totalDays

        appendLine("Period: $periodType ($start to $end)")
        appendLine("Days elapsed: $daysElapsed of $totalDays")
        appendLine("Period complete: ${if (isCurrentPeriod) "no - still in progress" else "yes"}")
        summary.totalIncome?.let { appendLine("Reported monthly income: ${it.money()}") }
        appendLine("Total spent so far: ${summary.totalSpent.money()}")

        if (summary.categories.isEmpty()) {
            appendLine("No spending was recorded for this period.")
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

    private fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
}