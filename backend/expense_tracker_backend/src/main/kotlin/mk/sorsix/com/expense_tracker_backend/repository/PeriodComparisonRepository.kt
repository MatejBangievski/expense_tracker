package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PeriodComparison
import org.springframework.data.jpa.repository.JpaRepository

interface PeriodComparisonRepository : JpaRepository<PeriodComparison, Long> {
    fun findByCurrentSummaryIdAndPreviousSummaryId(
        currentSummaryId: Long,
        previousSummaryId: Long,
    ): PeriodComparison?
}