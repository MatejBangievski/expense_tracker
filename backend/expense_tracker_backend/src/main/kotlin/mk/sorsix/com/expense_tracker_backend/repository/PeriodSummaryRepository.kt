package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummary
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface PeriodSummaryRepository : JpaRepository<PeriodSummary, Long> {
    fun findByUserIdAndPeriodTypeAndPeriodStart(
        userId: Long,
        periodType: PeriodType,
        periodStart: LocalDate,
    ): PeriodSummary?
}
