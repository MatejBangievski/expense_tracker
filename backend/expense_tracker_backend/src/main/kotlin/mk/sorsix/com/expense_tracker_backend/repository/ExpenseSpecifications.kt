package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.Expense
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object ExpenseSpecifications {

    fun belongsToUser(userId: Long): Specification<Expense> =
        Specification { root, _, cb -> cb.equal(root.get<Long>("user").get<Long>("id"), userId) }

    fun categoryNameEquals(categoryName: String?): Specification<Expense>? =
        categoryName?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb ->
                cb.equal(cb.lower(root.get<Category>("category").get("name")), it.lowercase())
            }
        }

    fun betweenDates(start: LocalDate?, end: LocalDate?): Specification<Expense>? =
        if (start != null && end != null) {
            Specification { root, _, cb -> cb.between(root.get("expenseDate"), start, end) }
        } else null

    fun descriptionContains(search: String?): Specification<Expense>? =
        search?.takeIf { it.isNotBlank() }?.let {
            Specification { root, _, cb -> cb.like(cb.lower(root.get("description")), "%${it.lowercase()}%") }
        }
}