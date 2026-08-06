package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.Expense
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategorySummary
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseSummary
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSpendingSummary
import mk.sorsix.com.expense_tracker_backend.domain.dto.SubcategorySummary
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class PeriodSummaryService(
    private val expenseRepository: ExpenseRepository,
) {

    fun summarize(
        userId: Long,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        totalIncome: BigDecimal? = null,
    ): PeriodSpendingSummary {
        val expenses = expenseRepository.findByUserIdAndExpenseDateBetween(userId, periodStart, periodEnd)

        val categories = expenses.groupBy { topLevelCategoryOf(it.category) }
            .map { (topCategory, topExpenses) ->
                val subcategories = topExpenses.groupBy { it.category }
                    .map { (leafCategory, leafExpenses) -> toSubcategorySummary(leafCategory, leafExpenses) }
                val categoryTotal = subcategories.fold(BigDecimal.ZERO) { acc, sub -> acc + sub.totalAmount }
                CategorySummary(
                    categoryName = topCategory.name,
                    totalAmount = categoryTotal,
                    subcategories = subcategories,
                )
            }

        return PeriodSpendingSummary(
            userId = userId,
            periodStart = periodStart,
            periodEnd = periodEnd,
            totalIncome = totalIncome,
            categories = categories,
        )
    }

    private fun toSubcategorySummary(
        leafCategory: Category,
        expenses: List<Expense>,
    ): SubcategorySummary {
        val total = expenses.fold(BigDecimal.ZERO) { acc, expense -> acc + expense.amount }
        return SubcategorySummary(
            subcategoryName = leafCategory.name,
            totalAmount = total,
            expenseCount = expenses.size,
            expenses = expenses.map { ExpenseSummary(it.description, it.amount, it.expenseDate) },
        )
    }

    private fun topLevelCategoryOf(category: Category): Category = category.parentCategory ?: category
}