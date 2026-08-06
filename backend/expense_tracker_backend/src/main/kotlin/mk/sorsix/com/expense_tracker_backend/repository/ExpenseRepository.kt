package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.Expense
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findByUserIdAndExpenseDateBetween(userId: Long, periodStart: LocalDate, periodEnd: LocalDate): List<Expense>
}