package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.CreateCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.CategoryResponse
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val currentUserProvider: CurrentUserProvider,
) {
    @GetMapping
    fun listAllCategories(
        @AuthenticationPrincipal userDetails: UserDetails,
    ): List<CategoryResponse> = categoryService.listAllCategories(currentUserProvider.resolve(userDetails))

    @PostMapping
    fun createCategory(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CategoryRequest,
    ): ResponseEntity<*> =
        when (val result = categoryService.createCategory(currentUserProvider.resolve(userDetails), request)) {
            is CreateCategoryResult.Success -> ResponseEntity.ok(result.category)
            CreateCategoryResult.NameAlreadyExists ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "Category name already exists"))

            CreateCategoryResult.ParentCategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Parent category not found"))
        }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestBody request: CategoryRequest,
    ): ResponseEntity<*> =
        when (val result = categoryService.updateCategory(currentUserProvider.resolve(userDetails), id, request)) {
            is UpdateCategoryResult.Success -> ResponseEntity.ok(result.category)
            is UpdateCategoryResult.CategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Category not found"))

            is UpdateCategoryResult.ParentCategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Parent category not found"))

            is UpdateCategoryResult.NotOwner ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "You do not own this category"))

            is UpdateCategoryResult.CannotEditDefault ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Default categories cannot be edited"))
        }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<*> =
        when (val result = categoryService.deleteCategory(currentUserProvider.resolve(userDetails), id)) {
            is DeleteCategoryResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteCategoryResult.CategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Category not found"))

            is DeleteCategoryResult.NotOwner ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "You do not own this category"))

            is DeleteCategoryResult.CannotDeleteDefault ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "Default categories cannot be deleted"))

            is DeleteCategoryResult.InUse ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "Category is in use and cannot be deleted"))
        }
}
