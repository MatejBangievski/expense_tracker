package mk.sorsix.com.expense_tracker_backend.domain.dto

data class CategoryRequest(
    val name: String,
    val parentCategoryId: Long? = null
)

data class CategoryResponse(
    val id: Long,
    val name: String,
    val parentCategoryId: Long?,
    val parentCategoryName: String?,
    val isDefault: Boolean
)