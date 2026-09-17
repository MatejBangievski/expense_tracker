package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.Category
import mk.sorsix.com.expense_tracker_backend.domain.CreateCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryResponse
import mk.sorsix.com.expense_tracker_backend.repository.BudgetRepository
import mk.sorsix.com.expense_tracker_backend.repository.CategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.ExpenseRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
) {
    fun findCategoryById(id: Long): Category? = categoryRepository.findByIdOrNull(id)

    fun rootOf(
        category: Category,
        byId: Map<Long, Category>,
    ): Category {
        var current = category
        val seen = mutableSetOf(current.id)
        while (true) {
            val parentId = current.parentCategory?.id ?: return current
            if (!seen.add(parentId)) return current
            current = byId[parentId] ?: return current
        }
    }

    fun categoriesById(userId: Long): Map<Long, Category> = categoryRepository.findByUserIsNullOrUserId(userId).associateBy { it.id }

    fun listAllCategories(user: User): List<CategoryResponse> = categoryRepository.findByUserIsNullOrUserId(user.id).map { it.toResponse() }

    fun createCategory(
        user: User,
        request: CategoryRequest,
    ): CreateCategoryResult {
        if (categoryRepository.findByNameIgnoreCase(request.name) != null) {
            return CreateCategoryResult.NameAlreadyExists
        }

        val parent =
            request.parentCategoryId?.let {
                categoryRepository.findByIdOrNull(it) ?: return CreateCategoryResult.ParentCategoryNotFound
            }

        val savedCategory =
            categoryRepository.save(
                Category(
                    name = request.name,
                    parentCategory = parent,
                    user = user,
                ),
            )
        return CreateCategoryResult.Success(savedCategory.toResponse())
    }

    fun updateCategory(
        user: User,
        id: Long,
        request: CategoryRequest,
    ): UpdateCategoryResult {
        val existingCategory =
            findCategoryById(id)
                ?: return UpdateCategoryResult.CategoryNotFound

        val categoryOwner =
            existingCategory.user
                ?: return UpdateCategoryResult.CannotEditDefault

        if (categoryOwner.id != user.id) {
            return UpdateCategoryResult.NotOwner
        }
        val parent =
            request.parentCategoryId?.let {
                categoryRepository.findById(it).orElse(null) ?: return UpdateCategoryResult.ParentCategoryNotFound
            }

        val updated = categoryRepository.save(existingCategory.copy(name = request.name, parentCategory = parent))
        return UpdateCategoryResult.Success(updated.toResponse())
    }

    fun deleteCategory(
        user: User,
        id: Long,
    ): DeleteCategoryResult {
        val existingCategory = findCategoryById(id) ?: return DeleteCategoryResult.CategoryNotFound
        val categoryOwner = existingCategory.user ?: return DeleteCategoryResult.CannotDeleteDefault
        if (categoryOwner.id != user.id) {
            return DeleteCategoryResult.NotOwner
        }
        val isInUse = expenseRepository.existsByCategoryId(id) || budgetRepository.existsByCategoryId(id)
        if (isInUse) {
            return DeleteCategoryResult.InUse
        }
        categoryRepository.delete(existingCategory)
        return DeleteCategoryResult.Success
    }

    private fun Category.toResponse() =
        CategoryResponse(
            id = id,
            name = name,
            parentCategoryId = parentCategory?.id,
            parentCategoryName = parentCategory?.name,
            isDefault = user == null,
        )
}
