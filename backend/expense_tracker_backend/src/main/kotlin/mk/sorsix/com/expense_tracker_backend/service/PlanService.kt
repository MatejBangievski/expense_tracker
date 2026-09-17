package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.CreatePlanResult
import mk.sorsix.com.expense_tracker_backend.domain.DeletePlanResult
import mk.sorsix.com.expense_tracker_backend.domain.Plan
import mk.sorsix.com.expense_tracker_backend.domain.UpdatePlanResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreatePlanRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.PlanResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdatePlanRequest
import mk.sorsix.com.expense_tracker_backend.repository.PlanItemRepository
import mk.sorsix.com.expense_tracker_backend.repository.PlanRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PlanService(
    private val planItemRepository: PlanItemRepository,
    private val planRepository: PlanRepository,
) {
    fun findPlanById(id: Long): Plan? = planRepository.findByIdOrNull(id)

    fun listPlans(user: User): List<PlanResponse> = planRepository.findByUserId(user.id).map { it.toResponse() }

    fun createPlan(
        user: User,
        request: CreatePlanRequest,
    ): CreatePlanResult {
        if (request.endDate.isBefore(request.startDate)) {
            return CreatePlanResult.InvalidDateRange
        }

        val saved =
            planRepository.save(
                Plan(
                    user = user,
                    name = request.name,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    totalBudget = request.totalBudget,
                ),
            )

        return CreatePlanResult.Success(saved.toResponse())
    }

    fun updatePlan(
        user: User,
        planId: Long,
        request: UpdatePlanRequest,
    ): UpdatePlanResult {
        val existing =
            findPlanById(planId)
                ?: return UpdatePlanResult.PlanNotFound

        if (existing.user.id != user.id) {
            return UpdatePlanResult.NotOwner
        }

        if (request.endDate.isBefore(request.startDate)) {
            return UpdatePlanResult.InvalidDateRange
        }

        val updated =
            planRepository.save(
                existing.copy(
                    name = request.name,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    totalBudget = request.totalBudget,
                ),
            )

        return UpdatePlanResult.Success(updated.toResponse())
    }

    fun deletePlan(
        user: User,
        planId: Long,
    ): DeletePlanResult {
        val existing =
            findPlanById(planId)
                ?: return DeletePlanResult.PlanNotFound

        if (existing.user.id != user.id) {
            return DeletePlanResult.NotOwner
        }

        planRepository.delete(existing)
        return DeletePlanResult.Success
    }

    private fun Plan.toResponse(): PlanResponse {
        val totalPlanned =
            planItemRepository
                .findByPlanId(id)
                .fold(BigDecimal.ZERO) { sum, item -> sum + item.plannedAmount }

        return PlanResponse(
            id = id,
            name = name,
            startDate = startDate,
            endDate = endDate,
            totalBudget = totalBudget,
            totalPlanned = totalPlanned,
        )
    }
}
