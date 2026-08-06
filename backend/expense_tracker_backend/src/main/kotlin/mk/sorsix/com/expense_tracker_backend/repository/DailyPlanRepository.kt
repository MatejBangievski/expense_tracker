package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.DailyPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyPlanRepository : JpaRepository<DailyPlan, Long> {
    fun findByPlanId(planId: Long): List<DailyPlan>
    fun findByPlanIdAndDate(planId: Long, date: LocalDate): DailyPlan?
}