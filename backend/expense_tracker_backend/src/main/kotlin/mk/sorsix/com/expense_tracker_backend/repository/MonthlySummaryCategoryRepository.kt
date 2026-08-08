package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.MonthlySummaryCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MonthlySummaryCategoryRepository : JpaRepository<MonthlySummaryCategory, Long> {
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("delete from MonthlySummaryCategory c where c.monthlySummary.id = :summaryId")
    fun deleteAllBySummaryId(@Param("summaryId") summaryId: Long): Int

    @Query("select c from MonthlySummaryCategory c join fetch c.category where c.monthlySummary.id = :summaryId")
    fun findAllWithCategoryBySummaryId(@Param("summaryId") summaryId: Long): List<MonthlySummaryCategory>
}
