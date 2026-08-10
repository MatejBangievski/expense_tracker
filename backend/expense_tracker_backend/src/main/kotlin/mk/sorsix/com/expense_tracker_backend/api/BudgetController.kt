package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.CreateBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdateBudgetResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.BudgetResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreateBudgetRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateBudgetRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.BudgetService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("api/budgets")
class BudgetController(
    private val budgetService: BudgetService,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam month: LocalDate
    ): List<BudgetResponse> =
        budgetService.listBudgetsByMonth(currentUserProvider.resolve(userDetails), month)

    @PostMapping
    fun create(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CreateBudgetRequest
    ): ResponseEntity<*> =
        when (val result = budgetService.createBudget(currentUserProvider.resolve(userDetails), request)) {
            is CreateBudgetResult.Success -> ResponseEntity.ok(result.budget)
            is CreateBudgetResult.CategoryNotFound -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Category not found"))

            is CreateBudgetResult.AlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "A budget already exists for this category and month"))
        }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestBody request: UpdateBudgetRequest
    ): ResponseEntity<*> =
        when (val result = budgetService.updateBudget(currentUserProvider.resolve(userDetails), id, request)) {
            is UpdateBudgetResult.Success -> ResponseEntity.ok(result.budget)
            is UpdateBudgetResult.BudgetNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Budget not found"))

            is UpdateBudgetResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this budget"))
        }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<*> =
        when (val result = budgetService.deleteBudget(currentUserProvider.resolve(userDetails), id)) {
            is DeleteBudgetResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteBudgetResult.BudgetNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Budget not found"))

            is DeleteBudgetResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this budget"))
        }
}