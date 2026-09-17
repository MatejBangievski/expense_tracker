package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.CreatePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.DeletePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.PlanItem
import mk.sorsix.com.expense_tracker_backend.domain.UpdatePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreatePlanItemRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.PlanItemResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdatePlanItemRequest
import mk.sorsix.com.expense_tracker_backend.repository.DailyPlanRepository
import mk.sorsix.com.expense_tracker_backend.repository.PlanItemRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PlanItemService(
    private val planItemRepository: PlanItemRepository,
    private val categoryService: CategoryService,
    private val planService: PlanService,
    private val dailyPlanRepository: DailyPlanRepository,
) {
    fun findById(id: Long): PlanItem? = planItemRepository.findByIdOrNull(id)

    fun listPlanItems(
        user: User,
        planId: Long,
    ): List<PlanItemResponse>? {
        val plan = planService.findPlanById(planId) ?: return null
        if (plan.user.id != user.id) return null
        return planItemRepository.findByPlanId(planId).map { it.toResponse() }
    }

    fun createPlanItem(
        user: User,
        planId: Long,
        request: CreatePlanItemRequest,
    ): CreatePlanItemResult {
        val plan =
            planService.findPlanById(planId)
                ?: return CreatePlanItemResult.PlanNotFound

        if (plan.user.id != user.id) {
            return CreatePlanItemResult.NotOwner
        }

        val currentTotal =
            planItemRepository
                .findByPlanId(planId)
                .fold(BigDecimal.ZERO) { sum, item -> sum + item.plannedAmount }

        val dailyPlan = dailyPlanRepository.findByPlanIdAndDate(planId, request.plannedDate)
        if (dailyPlan != null) {
            val currentDailyTotal =
                planItemRepository
                    .findByPlanId(planId)
                    .filter { it.plannedDate == request.plannedDate }
                    .fold(BigDecimal.ZERO) { sum, item -> sum + item.plannedAmount }

            val remainingDaily = dailyPlan.allocatedAmount - currentDailyTotal
            if (request.plannedAmount > remainingDaily && request.confirmOverBudget != true) {
                return CreatePlanItemResult.OverBudgetWarning(remainingDaily)
            }
        }

        val remaining = plan.totalBudget - currentTotal
        if (request.plannedAmount > remaining && request.confirmOverBudget != true) {
            return CreatePlanItemResult.OverBudgetWarning(remaining)
        }

        if (request.plannedDate.isBefore(plan.startDate) || request.plannedDate.isAfter(plan.endDate)) {
            return CreatePlanItemResult.DateOutsideRange
        }

        var category: Category? = null
        if (request.categoryId != null) {
            val foundCategory =
                categoryService.findCategoryById(request.categoryId)
                    ?: return CreatePlanItemResult.CategoryNotFound

            val categoryOwner = foundCategory.user
            if (categoryOwner != null && categoryOwner.id != user.id) {
                return CreatePlanItemResult.CategoryNotFound
            }

            category = foundCategory
        }

        val saved =
            planItemRepository.save(
                PlanItem(
                    plan = plan,
                    category = category,
                    description = request.description,
                    plannedDate = request.plannedDate,
                    plannedAmount = request.plannedAmount,
                ),
            )

        return CreatePlanItemResult.Success(saved.toResponse())
    }

    fun updatePlanItem(
        user: User,
        itemId: Long,
        request: UpdatePlanItemRequest,
    ): UpdatePlanItemResult {
        val existing =
            planItemRepository.findById(itemId).orElse(null)
                ?: return UpdatePlanItemResult.ItemNotFound

        if (existing.plan.user.id != user.id) {
            return UpdatePlanItemResult.NotOwner
        }

        if (request.plannedDate.isBefore(existing.plan.startDate) || request.plannedDate.isAfter(existing.plan.endDate)) {
            return UpdatePlanItemResult.DateOutsideRange
        }

        val dailyPlan = dailyPlanRepository.findByPlanIdAndDate(existing.plan.id, request.plannedDate)
        if (dailyPlan != null) {
            val otherDailyTotal =
                planItemRepository
                    .findByPlanId(existing.plan.id)
                    .filter { it.id != itemId && it.plannedDate == request.plannedDate }
                    .fold(BigDecimal.ZERO) { sum, item -> sum + item.plannedAmount }

            val remainingDaily = dailyPlan.allocatedAmount - otherDailyTotal
            if (request.plannedAmount > remainingDaily && request.confirmOverBudget != true) {
                return UpdatePlanItemResult.OverBudgetWarning(remainingDaily)
            }
        }

        val otherItemsTotal =
            planItemRepository
                .findByPlanId(existing.plan.id)
                .filter { it.id != itemId }
                .fold(BigDecimal.ZERO) { sum, item -> sum + item.plannedAmount }

        val remaining = existing.plan.totalBudget - otherItemsTotal

        if (request.plannedAmount > remaining && request.confirmOverBudget != true) {
            return UpdatePlanItemResult.OverBudgetWarning(remaining)
        }

        var category: Category? = null
        if (request.categoryId != null) {
            val foundCategory =
                categoryService.findCategoryById(request.categoryId)
                    ?: return UpdatePlanItemResult.CategoryNotFound

            val categoryOwner = foundCategory.user
            if (categoryOwner != null && categoryOwner.id != user.id) {
                return UpdatePlanItemResult.CategoryNotFound
            }

            category = foundCategory
        }

        val updated =
            planItemRepository.save(
                existing.copy(
                    category = category,
                    description = request.description,
                    plannedDate = request.plannedDate,
                    plannedAmount = request.plannedAmount,
                ),
            )

        return UpdatePlanItemResult.Success(updated.toResponse())
    }

    fun deletePlanItem(
        user: User,
        itemId: Long,
    ): DeletePlanItemResult {
        val existing = findById(itemId) ?: return DeletePlanItemResult.ItemNotFound
        if (existing.plan.user.id != user.id) {
            return DeletePlanItemResult.NotOwner
        }
        planItemRepository.delete(existing)
        return DeletePlanItemResult.Success
    }

    private fun PlanItem.toResponse() =
        PlanItemResponse(
            id = id,
            planId = plan.id,
            categoryId = category?.id,
            categoryName = category?.name,
            description = description,
            plannedDate = plannedDate,
            plannedAmount = plannedAmount,
        )
}
