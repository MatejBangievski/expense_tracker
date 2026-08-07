package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.PlanItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlanItemRepository : JpaRepository<PlanItem, Long> {
    fun findByPlanId(planId: Long): List<PlanItem>
}