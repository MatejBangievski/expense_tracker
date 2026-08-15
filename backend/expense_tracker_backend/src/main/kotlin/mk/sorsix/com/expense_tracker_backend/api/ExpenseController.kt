package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.CreateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteExpenseResult

import mk.sorsix.com.expense_tracker_backend.domain.UpdateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseFilter
import mk.sorsix.com.expense_tracker_backend.domain.dto.ExpenseResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateExpenseRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.ExpenseService
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
@RequestMapping("api/expenses")
class ExpenseController(
    private val expenseService: ExpenseService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun getExpenses(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) categoryName: String?,
        @RequestParam(required = false) periodStart: LocalDate?,
        @RequestParam(required = false) periodEnd: LocalDate?,
        @RequestParam(required = false) search: String?
    ): List<ExpenseResponse> {
        val filter = ExpenseFilter(categoryName, periodStart, periodEnd, search)
        return expenseService.listExpenses(currentUserProvider.resolve(userDetails), filter)
    }

    @PostMapping
    fun createExpense(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CreateExpenseRequest
    ): ResponseEntity<*> =
        when (val result = expenseService.createExpense(currentUserProvider.resolve(userDetails), request)) {
            is CreateExpenseResult.Success -> ResponseEntity.ok(result.expense)
            is CreateExpenseResult.CategoryNotFound -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Category not found"))
        }

    @PutMapping("/{id}")
    fun updateExpense(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestBody request: UpdateExpenseRequest
    ): ResponseEntity<*> =
        when (val result = expenseService.updateExpense(currentUserProvider.resolve(userDetails), id, request)) {
            is UpdateExpenseResult.Success -> ResponseEntity.ok(result.expense)
            is UpdateExpenseResult.ExpenseNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Expense not found"))

            is UpdateExpenseResult.CategoryNotFound -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Category not found"))

            is UpdateExpenseResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this expense"))

            is UpdateExpenseResult.ExpenseLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "This expense is locked because its week has been summarized"))
        }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<*> =
        when (val result = expenseService.deleteExpense(currentUserProvider.resolve(userDetails), id)) {
            is DeleteExpenseResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteExpenseResult.ExpenseNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Expense not found"))

            is DeleteExpenseResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this expense"))

            is DeleteExpenseResult.ExpenseLocked -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "This expense is locked because its week has been summarized"))
        }
}