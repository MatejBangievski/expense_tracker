package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.AvailablePeriodsResult
import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.CurrentSpendingResult
import mk.sorsix.com.expense_tracker_backend.domain.FindPeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummary
import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummaryCategory
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.AvailablePeriod
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.SubCategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseSpecifications
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryCategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate


@Service
class PeriodSummaryService(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val categoryService: CategoryService,
    private val userRepository: UserRepository,
    private val periodSummaryRepository: PeriodSummaryRepository,
    private val periodSummaryCategoryRepository: PeriodSummaryCategoryRepository,
    private val clock: Clock,
) {
    @Transactional
    fun generateForUser(userId: Long, periodType: PeriodType, date: LocalDate, totalIncomeOverride: BigDecimal? = null): GeneratePeriodSummaryResult {
        val periodStart = periodType.startOf(date)
        val periodEnd = periodType.endOf(periodStart)

        val user = userRepository.findById(userId).orElse(null)
            ?: return GeneratePeriodSummaryResult.UserNotFound

        val expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, periodStart, periodEnd)
        val subCategories = expenses.groupBy { it.category }
            .map { (category, categoryExpenses) ->
                subCategorySpend(
                    categoryId = category.id,
                    totalAmount = categoryExpenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount },
                    expenseCount = categoryExpenses.size,
                )
            }
        val totalSpent = subCategories.fold(BigDecimal.ZERO) { acc, subCategory -> acc + subCategory.totalAmount }
        val income = totalIncomeOverride ?: user.monthlySalary

        val existing = periodSummaryRepository.findByUserIdAndPeriodTypeAndPeriodStart(userId, periodType, periodStart)
        val summary = periodSummaryRepository.save(
            (existing ?: PeriodSummary(user = user, periodType = periodType, periodStart = periodStart)).copy(
                totalSpent = totalSpent,
                totalIncome = income,
            )
        )

        periodSummaryCategoryRepository.deleteAllBySummaryId(summary.id)

        val categoriesById = categoryRepository.findByUserIsNullOrUserId(userId).associateBy { it.id }
        periodSummaryCategoryRepository.saveAll(
            subCategories.mapNotNull { sc ->
                categoriesById[sc.categoryId]?.let { category ->
                    PeriodSummaryCategory(
                        periodSummary = summary,
                        category = category,
                        totalAmount = sc.totalAmount,
                        expenseCount = sc.expenseCount,
                    )
                }
            }
        )

        if (periodType == PeriodType.WEEK && periodEnd.isBefore(LocalDate.now(clock))) {
            expenseRepository.deactivateByUserAndDateRange(userId, periodStart, periodEnd)
        }

        return GeneratePeriodSummaryResult.Success(
            buildResponse(summary.id, userId, periodType, periodStart, income, totalSpent, subCategories, categoriesById),
        )
    }

    @Transactional(readOnly = true)
    fun find(userId: Long, periodType: PeriodType, date: LocalDate): FindPeriodSummaryResult {
        val periodStart = periodType.startOf(date)
        val summary = periodSummaryRepository.findByUserIdAndPeriodTypeAndPeriodStart(userId, periodType, periodStart)
            ?: return FindPeriodSummaryResult.SummaryNotFound
        val categoriesById = categoryService.categoriesById(userId)
        val subCategories = periodSummaryCategoryRepository.findAllWithCategoryBySummaryId(summary.id)
            .map { subCategorySpend(it.category.id, it.totalAmount, it.expenseCount) }
        return FindPeriodSummaryResult.Success(
            buildResponse(
                summary.id, summary.user.id, summary.periodType, summary.periodStart,
                summary.totalIncome, summary.totalSpent, subCategories, categoriesById,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun availablePeriods(userId: Long, periodType: PeriodType): AvailablePeriodsResult {
        if (!userRepository.existsById(userId)) return AvailablePeriodsResult.UserNotFound
        val periods = expenseRepository.findAll(ExpenseSpecifications.belongsToUser(userId))
            .groupBy { periodType.startOf(it.expenseDate) }
            .map { (periodStart, expenses) ->
                AvailablePeriod(periodStart, expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount })
            }
            .sortedByDescending { it.periodStart }
        return AvailablePeriodsResult.Success(periods)
    }

    @Transactional(readOnly = true)
    fun currentSpending(userId: Long): CurrentSpendingResult {
        if (!userRepository.existsById(userId)) return CurrentSpendingResult.UserNotFound
        return CurrentSpendingResult.Success(currentMonthSpending(userId))
    }

    @Transactional(readOnly = true)
    fun currentMonthSpending(userId: Long): PeriodSummaryResponse {
        val periodStart = PeriodType.MONTH.startOf(LocalDate.now(clock))
        val periodEnd = PeriodType.MONTH.endOf(periodStart)
        val user = userRepository.findById(userId).orElse(null)
            ?: return PeriodSummaryResponse(0, userId, PeriodType.MONTH, periodStart, null, BigDecimal.ZERO, emptyList())

        val subCategories = expenseRepository.findByUserIdAndExpenseDateBetween(userId, periodStart, periodEnd)
            .groupBy { it.category }
            .map { (category, expenses) ->
                subCategorySpend(category.id, expenses.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }, expenses.size)
            }
        val totalSpent = subCategories.fold(BigDecimal.ZERO) { acc, sc -> acc + sc.totalAmount }
        val categoriesById = categoryService.categoriesById(userId)
        return buildResponse(0, userId, PeriodType.MONTH, periodStart, user.monthlySalary, totalSpent, subCategories, categoriesById)
    }

    private fun buildResponse(summaryId: Long, userId: Long, periodType: PeriodType, periodStart: LocalDate, totalIncome: BigDecimal?, totalSpent: BigDecimal, subCategories: List<subCategorySpend>, categoriesById: Map<Long, Category>): PeriodSummaryResponse {
        val categories = subCategories
            .mapNotNull { subCategory -> categoriesById[subCategory.categoryId]?.let { it to subCategory } }
            .groupBy { (category, _) -> categoryService.rootOf(category, categoriesById) }
            .map { (root, entries) ->
                CategorySummaryResponse(
                    categoryId = root.id,
                    categoryName = root.name,
                    totalAmount = entries.fold(BigDecimal.ZERO) { acc, (_, subCategory) -> acc + subCategory.totalAmount },
                    expenseCount = entries.sumOf { (_, subCategory) -> subCategory.expenseCount },
                    subcategories = entries
                        .map { (category, subCategory) ->
                            SubCategorySummaryResponse(category.id, category.name, subCategory.totalAmount, subCategory.expenseCount)
                        }
                        .sortedByDescending { it.totalAmount },
                )
            }
            .sortedByDescending { it.totalAmount }

        return PeriodSummaryResponse(
            summaryId = summaryId,
            userId = userId,
            periodType = periodType,
            periodStart = periodStart,
            totalIncome = totalIncome,
            totalSpent = totalSpent,
            categories = categories,
        )
    }
}

private data class subCategorySpend(val categoryId: Long, val totalAmount: BigDecimal, val expenseCount: Int)