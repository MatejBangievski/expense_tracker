package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.FindMonthlySummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.GenerateMonthlySummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.MonthlySummary
import mk.sorsix.com.expense_tracker_backend.domain.MonthlySummaryCategory
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.MonthlySummaryResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.SubCategorySummaryResponse
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySummaryCategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.MonthlySummaryRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate


@Service
class MonthlySummaryService(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val monthlySummaryRepository: MonthlySummaryRepository,
    private val monthlySummaryCategoryRepository: MonthlySummaryCategoryRepository,
) {
    @Transactional
    fun generateForUser(userId: Long, month: LocalDate, totalIncomeOverride: BigDecimal? = null): GenerateMonthlySummaryResult {
        val monthStart = month.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)

        val user = userRepository.findById(userId).orElse(null)
            ?: return GenerateMonthlySummaryResult.UserNotFound

        val expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, monthStart, monthEnd)
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

        val existing = monthlySummaryRepository.findByUserIdAndSummaryMonth(userId, monthStart)
        val summary = monthlySummaryRepository.save(
            (existing ?: MonthlySummary(user = user, summaryMonth = monthStart)).copy(
                totalSpent = totalSpent,
                totalIncome = income,
            )
        )

        monthlySummaryCategoryRepository.deleteAllBySummaryId(summary.id)

        val categoriesById = categoryRepository.findAll().associateBy { it.id }
        monthlySummaryCategoryRepository.saveAll(
            subCategories.mapNotNull { sc ->
                categoriesById[sc.categoryId]?.let { category ->
                    MonthlySummaryCategory(
                        monthlySummary = summary,
                        category = category,
                        totalAmount = sc.totalAmount,
                        expenseCount = sc.expenseCount,
                    )
                }
            }
        )

        return GenerateMonthlySummaryResult.Success(toResponse(summary, subCategories, categoriesById))
    }

    @Transactional(readOnly = true)
    fun find(userId: Long, month: LocalDate): FindMonthlySummaryResult {
        val monthStart = month.withDayOfMonth(1)
        val summary = monthlySummaryRepository.findByUserIdAndSummaryMonth(userId, monthStart)
            ?: return FindMonthlySummaryResult.SummaryNotFound
        val categoriesById = categoryRepository.findAll().associateBy { it.id }
        val subCategories = monthlySummaryCategoryRepository.findAllWithCategoryBySummaryId(summary.id)
            .map { subCategorySpend(it.category.id, it.totalAmount, it.expenseCount) }
        return FindMonthlySummaryResult.Success(toResponse(summary, subCategories, categoriesById))
    }

    private fun toResponse(summary: MonthlySummary, subCategories: List<subCategorySpend>, categoriesById: Map<Long, Category>, ): MonthlySummaryResponse {
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

        return MonthlySummaryResponse(
            summaryId = summary.id,
            userId = summary.user.id,
            summaryMonth = summary.summaryMonth,
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