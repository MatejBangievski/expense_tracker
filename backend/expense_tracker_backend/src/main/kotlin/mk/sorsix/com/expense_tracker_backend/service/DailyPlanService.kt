package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.CreateDailyPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.DailyPlan
import mk.sorsix.com.expense_tracker_backend.domain.DeleteDailyPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateDailyPlanResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.DailyPlanRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.DailyPlanResponse
import mk.sorsix.com.expense_tracker_backend.repository.DailyPlanRepository
import mk.sorsix.com.expense_tracker_backend.repository.PlanItemRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class DailyPlanService(
    private val dailyPlanRepository: DailyPlanRepository,
    private val planService: PlanService,
    private val planItemRepository: PlanItemRepository,
) {
    fun listDailyPlans(
        user: User,
        planId: Long,
    ): List<DailyPlanResponse>? {
        val plan = planService.findPlanById(planId) ?: return null
        if (plan.user.id != user.id) {
            return null
        }
        return dailyPlanRepository.findByPlanId(planId).map { it.toResponse() }
    }

    fun getDailyPlanById(planId: Long): DailyPlan? = dailyPlanRepository.findByIdOrNull(planId)

    fun createDailyPlan(
        user: User,
        planId: Long,
        request: DailyPlanRequest,
    ): CreateDailyPlanResult {
        val plan = planService.findPlanById(planId) ?: return CreateDailyPlanResult.PlanNotFound

        if (plan.user.id != user.id) {
            return CreateDailyPlanResult.NotOwner
        }

        if (request.date.isBefore(plan.startDate) || request.date.isAfter(plan.endDate)) {
            return CreateDailyPlanResult.DateOutsideRange
        }
        val existingPlan = dailyPlanRepository.findByPlanIdAndDate(planId, request.date)
        if (existingPlan != null) {
            return CreateDailyPlanResult.AlreadyExists
        }

        val currentAllocated =
            dailyPlanRepository
                .findByPlanId(planId)
                .fold(BigDecimal.ZERO) { sum, dp -> sum + dp.allocatedAmount }

        val remaining = plan.totalBudget - currentAllocated
        if (request.allocatedAmount > remaining && request.confirmOverBudget != true) {
            return CreateDailyPlanResult.OverBudgetWarning(remaining)
        }

        val dailyPlan =
            dailyPlanRepository.save(
                DailyPlan(
                    plan = plan,
                    date = request.date,
                    allocatedAmount = request.allocatedAmount,
                ),
            )
        return CreateDailyPlanResult.Success(dailyPlan.toResponse())
    }

    fun updateDailyPlan(
        user: User,
        dailyPlanId: Long,
        request: DailyPlanRequest,
    ): UpdateDailyPlanResult {
        val existingPlan = getDailyPlanById(dailyPlanId) ?: return UpdateDailyPlanResult.DailyPlanNotFound
        if (existingPlan.plan.user.id != user.id) {
            return UpdateDailyPlanResult.NotOwner
        }

        val updated = dailyPlanRepository.save(existingPlan.copy(allocatedAmount = request.allocatedAmount))
        return UpdateDailyPlanResult.Success(updated.toResponse())
    }

    fun deleteDailyPlan(
        user: User,
        dailyPlanId: Long,
    ): DeleteDailyPlanResult {
        val existing =
            dailyPlanRepository.findById(dailyPlanId).orElse(null)
                ?: return DeleteDailyPlanResult.DailyPlanNotFound

        if (existing.plan.user.id != user.id) {
            return DeleteDailyPlanResult.NotOwner
        }

        dailyPlanRepository.delete(existing)
        return DeleteDailyPlanResult.Success
    }

    private fun DailyPlan.toResponse(): DailyPlanResponse {
        val plannedAmount =
            planItemRepository.sumPlannedAmountByPlanAndDate(
                plan.id,
                date,
            )

        return DailyPlanResponse(
            id = id,
            planId = plan.id,
            date = date,
            allocatedAmount = allocatedAmount,
            actualSpent = plannedAmount,
        )
    }
}
