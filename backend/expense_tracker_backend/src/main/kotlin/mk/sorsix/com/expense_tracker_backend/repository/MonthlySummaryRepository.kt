package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.MonthlySummary
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MonthlySummaryRepository : JpaRepository<MonthlySummary, Long> {
    fun findByUserIdAndSummaryMonth(userId: Long, summaryMonth: LocalDate): MonthlySummary?
}
