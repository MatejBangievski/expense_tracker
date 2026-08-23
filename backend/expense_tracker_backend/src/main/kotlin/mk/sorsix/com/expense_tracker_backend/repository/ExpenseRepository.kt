package mk.sorsix.com.expense_tracker_backend.repository

import org.springframework.data.jpa.domain.Specification
import mk.sorsix.com.expense_tracker_backend.domain.Expense
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate

interface ExpenseRepository : JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {
    @EntityGraph(attributePaths = ["category", "user"])
    override fun findAll(spec: Specification<Expense>): List<Expense>
    fun existsByCategoryId(categoryId: Long): Boolean

    @EntityGraph(attributePaths = ["category"])
    fun findByUserIdAndExpenseDateBetween(userId: Long, periodStart: LocalDate, periodEnd: LocalDate): List<Expense>

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.user.id = :userId AND e.category.id = :categoryId AND e.expenseDate BETWEEN :start AND :end
    """
    )
    fun sumAmountByUserAndCategoryAndDateRange(
        @Param("userId") userId: Long,
        @Param("categoryId") categoryId: Long,
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate
    ): BigDecimal

    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
        UPDATE Expense e SET e.active = false
        WHERE e.user.id = :userId AND e.expenseDate BETWEEN :start AND :end
    """)
    fun deactivateByUserAndDateRange(
        @Param("userId") userId: Long,
        @Param("start") start: LocalDate,
        @Param("end") end: LocalDate,
    ): Int
}