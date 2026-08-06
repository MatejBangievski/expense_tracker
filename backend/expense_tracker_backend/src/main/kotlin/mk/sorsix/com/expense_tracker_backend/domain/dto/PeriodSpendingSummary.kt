package mk.sorsix.com.expense_tracker_backend.domain.dto

import java.math.BigDecimal
import java.time.LocalDate

data class PeriodSpendingSummary(
    val userId: Long,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalIncome: BigDecimal? = null,
    val categories: List<CategorySummary> = emptyList(),
) {
    val totalSpent: BigDecimal
        get() = categories.fold(BigDecimal.ZERO) { acc, category -> acc + category.totalAmount }

    override fun toString(): String = buildString {
        appendLine("User ID: $userId")
        appendLine("Period: $periodStart to $periodEnd")
        totalIncome?.let { appendLine("Reported income for the period: $it") }
        appendLine()

        if (categories.isEmpty()) {
            appendLine("No spending categories were reported for this period.")
            return@buildString
        }

        appendLine("Spending by category:")
        categories.forEach { category ->
            appendLine("- ${category.categoryName} (total: ${category.totalAmount})")
            category.subcategories.forEach { sub ->
                appendLine("    - ${sub.subcategoryName}: ${sub.totalAmount} across ${sub.expenseCount} expense(s)")
                sub.expenses.forEach { exp ->
                    val label = exp.description ?: "(no description)"
                    appendLine("        * $label on ${exp.expenseDate} = ${exp.amount}")
                }
            }
        }
    }
}