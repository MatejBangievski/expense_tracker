package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PlanItem
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface PlanItemRepository : JpaRepository<PlanItem, Long> {
    @EntityGraph(attributePaths = ["category", "plan"])
    fun findByPlanId(planId: Long): List<PlanItem>

    @Query(
        """
    SELECT COALESCE(SUM(p.plannedAmount), 0)
    FROM PlanItem p
    WHERE p.plan.id = :planId
      AND p.plannedDate = :date
""",
    )
    fun sumPlannedAmountByPlanAndDate(
        @Param("planId") planId: Long,
        @Param("date") date: LocalDate,
    ): BigDecimal
}
