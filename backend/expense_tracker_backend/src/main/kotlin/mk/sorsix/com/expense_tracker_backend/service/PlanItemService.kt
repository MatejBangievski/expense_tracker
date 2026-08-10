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
import mk.sorsix.com.expense_tracker_backend.repository.PlanItemRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class PlanItemService(
    private val planItemRepository: PlanItemRepository,
    private val categoryService: CategoryService,
    private val planService: PlanService
) {
    fun findById(id: Long): PlanItem? {
        return planItemRepository.findByIdOrNull(id)
    }

    fun listPlanItems(user: User, planId: Long): List<PlanItemResponse>? {
        val plan = planService.findPlanById(planId) ?: return null
        if (plan.user.id != user.id) return null
        return planItemRepository.findByPlanId(planId).map { it.toResponse() }
    }
    fun createPlanItem(user: User, planId: Long, request: CreatePlanItemRequest): CreatePlanItemResult {
        val plan = planService.findPlanById(planId)
            ?: return CreatePlanItemResult.PlanNotFound

        if (plan.user.id != user.id) {
            return CreatePlanItemResult.NotOwner
        }

        var category: Category? = null
        if (request.categoryId != null) {
            val foundCategory = categoryService.findCategoryById(request.categoryId)
                ?: return CreatePlanItemResult.CategoryNotFound

            val categoryOwner = foundCategory.user
            if (categoryOwner != null && categoryOwner.id != user.id) {
                return CreatePlanItemResult.CategoryNotFound
            }

            category = foundCategory
        }

        val saved = planItemRepository.save(
            PlanItem(
                plan = plan,
                category = category,
                description = request.description,
                plannedDate = request.plannedDate,
                plannedAmount = request.plannedAmount
            )
        )

        return CreatePlanItemResult.Success(saved.toResponse())
    }

    fun updatePlanItem(user: User, itemId: Long, request: UpdatePlanItemRequest): UpdatePlanItemResult {
        val existing = planItemRepository.findById(itemId).orElse(null)
            ?: return UpdatePlanItemResult.ItemNotFound

        if (existing.plan.user.id != user.id) {
            return UpdatePlanItemResult.NotOwner
        }

        var category: Category? = null
        if (request.categoryId != null) {
            val foundCategory = categoryService.findCategoryById(request.categoryId)
                ?: return UpdatePlanItemResult.CategoryNotFound

            val categoryOwner = foundCategory.user
            if (categoryOwner != null && categoryOwner.id != user.id) {
                return UpdatePlanItemResult.CategoryNotFound
            }

            category = foundCategory
        }

        val updated = planItemRepository.save(
            existing.copy(
                category = category,
                description = request.description,
                plannedDate = request.plannedDate,
                plannedAmount = request.plannedAmount
            )
        )

        return UpdatePlanItemResult.Success(updated.toResponse())
    }

    fun deletePlanItem(user: User, itemId: Long): DeletePlanItemResult {
        val existing = findById(itemId) ?: return DeletePlanItemResult.ItemNotFound
        if (existing.plan.user.id != user.id) {
            return DeletePlanItemResult.NotOwner
        }
        planItemRepository.delete(existing)
        return DeletePlanItemResult.Success
    }

    private fun PlanItem.toResponse() = PlanItemResponse(
        id = id,
        planId = plan.id,
        categoryId = category?.id,
        categoryName = category?.name,
        description = description,
        plannedDate = plannedDate,
        plannedAmount = plannedAmount
    )
}