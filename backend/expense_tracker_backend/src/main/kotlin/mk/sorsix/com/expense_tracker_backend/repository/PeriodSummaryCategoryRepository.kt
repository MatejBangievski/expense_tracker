package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PeriodSummaryCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PeriodSummaryCategoryRepository : JpaRepository<PeriodSummaryCategory, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from PeriodSummaryCategory c where c.periodSummary.id = :summaryId")
    fun deleteAllBySummaryId(
        @Param("summaryId") summaryId: Long,
    ): Int

    @Query("select c from PeriodSummaryCategory c join fetch c.category where c.periodSummary.id = :summaryId")
    fun findAllWithCategoryBySummaryId(
        @Param("summaryId") summaryId: Long,
    ): List<PeriodSummaryCategory>
}
