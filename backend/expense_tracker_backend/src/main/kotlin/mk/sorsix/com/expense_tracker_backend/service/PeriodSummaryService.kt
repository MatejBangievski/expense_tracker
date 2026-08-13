package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.FindPeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummary
import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummaryCategory
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.SubCategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryCategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate


@Service
class PeriodSummaryService(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val periodSummaryRepository: PeriodSummaryRepository,
    private val periodSummaryCategoryRepository: PeriodSummaryCategoryRepository,
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

        return GeneratePeriodSummaryResult.Success(toResponse(summary, subCategories, categoriesById))
    }

    @Transactional(readOnly = true)
    fun find(userId: Long, periodType: PeriodType, date: LocalDate): FindPeriodSummaryResult {
        val periodStart = periodType.startOf(date)
        val summary = periodSummaryRepository.findByUserIdAndPeriodTypeAndPeriodStart(userId, periodType, periodStart)
            ?: return FindPeriodSummaryResult.SummaryNotFound
        val categoriesById = categoryRepository.findByUserIsNullOrUserId(userId).associateBy { it.id }
        val subCategories = periodSummaryCategoryRepository.findAllWithCategoryBySummaryId(summary.id)
            .map { subCategorySpend(it.category.id, it.totalAmount, it.expenseCount) }
        return FindPeriodSummaryResult.Success(toResponse(summary, subCategories, categoriesById))
    }

    private fun toResponse(summary: PeriodSummary, subCategories: List<subCategorySpend>, categoriesById: Map<Long, Category>, ): PeriodSummaryResponse {
        val categories = subCategories
            .mapNotNull { subCategory -> categoriesById[subCategory.categoryId]?.let { it to subCategory } }
            .groupBy { (category, _) -> rootOf(category, categoriesById) }
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
            summaryId = summary.id,
            userId = summary.user.id,
            periodType = summary.periodType,
            periodStart = summary.periodStart,
            totalIncome = summary.totalIncome,
            totalSpent = summary.totalSpent,
            categories = categories,
        )
    }

    private fun rootOf(subCategory: Category, categoriesById: Map<Long, Category>): Category {
        var current = subCategory
        val seen = mutableSetOf(current.id)
        while (true) {
            val parentId = current.parentCategory?.id ?: return current
            if (!seen.add(parentId)) return current
            current = categoriesById[parentId] ?: return current
        }
    }
}

private data class subCategorySpend(val categoryId: Long, val totalAmount: BigDecimal, val expenseCount: Int)