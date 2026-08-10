package mk.sorsix.com.expense_tracker_backend.domain

import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryResponse

sealed class CreateCategoryResult {
    data class Success(val category: CategoryResponse) : CreateCategoryResult()
    object ParentCategoryNotFound : CreateCategoryResult()
    object NameAlreadyExists : CreateCategoryResult()
}

sealed class UpdateCategoryResult {
    data class Success(val category: CategoryResponse) : UpdateCategoryResult()
    object CategoryNotFound : UpdateCategoryResult()
    object ParentCategoryNotFound : UpdateCategoryResult()
    object NotOwner : UpdateCategoryResult()
    object CannotEditDefault : UpdateCategoryResult()
}

sealed class DeleteCategoryResult {
    object Success : DeleteCategoryResult()
    object CategoryNotFound : DeleteCategoryResult()
    object NotOwner : DeleteCategoryResult()
    object CannotDeleteDefault : DeleteCategoryResult()
    object InUse : DeleteCategoryResult()
}