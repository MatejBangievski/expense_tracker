package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.Budget
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface BudgetRepository : JpaRepository<Budget, Long> {


    @EntityGraph(attributePaths = ["category", "user"])
    fun findByMonthlySavingId(monthlySavingId: Long): List<Budget>
    fun existsByCategoryId(categoryId: Long): Boolean
    fun findByMonthlySavingIdAndCategoryId(monthlySavingId: Long, categoryId: Long): Budget?
}