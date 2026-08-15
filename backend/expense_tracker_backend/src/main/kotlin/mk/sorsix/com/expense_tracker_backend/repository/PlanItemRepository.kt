package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PlanItem
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlanItemRepository : JpaRepository<PlanItem, Long> {
    @EntityGraph(attributePaths = ["category", "plan"])
    fun findByPlanId(planId: Long): List<PlanItem>
}