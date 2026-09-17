package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.CreateExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.DeleteExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.FindExpenseResult
import mk.sorsix.com.expense_tracker_backend.domain.RecentExpensesResult
import mk.sorsix.com.expense_tracker_backend.domain.TopCategoryResult
import mk.sorsix.com.expense_tracker_backend.domain.TotalSpentResult
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
    private val currentUserProvider: CurrentUserProvider,
) {
    @GetMapping
    fun getExpenses(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(required = false) categoryName: String?,
        @RequestParam(required = false) periodStart: LocalDate?,
        @RequestParam(required = false) periodEnd: LocalDate?,
        @RequestParam(required = false) search: String?,
    ): List<ExpenseResponse> {
        val filter = ExpenseFilter(categoryName, periodStart, periodEnd, search)
        return expenseService.listExpenses(currentUserProvider.resolve(userDetails), filter)
    }

    @GetMapping("/{id}")
    fun getById(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<*> =
        when (val result = expenseService.findById(currentUserProvider.resolve(userDetails), id)) {
            is FindExpenseResult.Success -> ResponseEntity.ok(result.expense)
            is FindExpenseResult.ExpenseNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Expense not found"))

            is FindExpenseResult.NotOwner ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "You do not own this expense"))
        }

    @PostMapping
    fun createExpense(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CreateExpenseRequest,
    ): ResponseEntity<*> =
        when (val result = expenseService.createExpense(currentUserProvider.resolve(userDetails), request)) {
            is CreateExpenseResult.Success -> ResponseEntity.ok(result.expense)
            is CreateExpenseResult.CategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Category not found"))

            is CreateExpenseResult.PlanNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Plan not found"))

            is CreateExpenseResult.OverBudgetWarning ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "over_budget", "message" to result.message))
        }

    @PutMapping("/{id}")
    fun updateExpense(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestBody request: UpdateExpenseRequest,
    ): ResponseEntity<*> =
        when (val result = expenseService.updateExpense(currentUserProvider.resolve(userDetails), id, request)) {
            is UpdateExpenseResult.Success -> ResponseEntity.ok(result.expense)
            is UpdateExpenseResult.ExpenseNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Expense not found"))

            is UpdateExpenseResult.CategoryNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Category not found"))

            is UpdateExpenseResult.NotOwner ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "You do not own this expense"))

            is UpdateExpenseResult.ExpenseLocked ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "This expense is locked because its week has been summarized"))

            is UpdateExpenseResult.PlanNotFound ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Plan not found"))

            is UpdateExpenseResult.OverBudgetWarning ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "over_budget", "message" to result.message))
        }

    @DeleteMapping("/{id}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
    ): ResponseEntity<*> =
        when (val result = expenseService.deleteExpense(currentUserProvider.resolve(userDetails), id)) {
            is DeleteExpenseResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteExpenseResult.ExpenseNotFound ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Expense not found"))

            is DeleteExpenseResult.NotOwner ->
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("error" to "You do not own this expense"))

            is DeleteExpenseResult.ExpenseLocked ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to "This expense is locked because its week has been summarized"))
        }

    @GetMapping("/recent")
    fun recent(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "5") limit: Int,
    ): ResponseEntity<*> =
        when (val result = expenseService.recentExpenses(currentUserProvider.resolve(userDetails), limit)) {
            is RecentExpensesResult.Success -> ResponseEntity.ok(result.expenses)
            is RecentExpensesResult.InvalidLimit ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "limit must be at least 1"))
        }

    @GetMapping("/total")
    fun total(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam periodStart: LocalDate,
        @RequestParam periodEnd: LocalDate,
    ): ResponseEntity<*> =
        when (val result = expenseService.totalSpent(currentUserProvider.resolve(userDetails), periodStart, periodEnd)) {
            is TotalSpentResult.Success -> ResponseEntity.ok(result.total)
            is TotalSpentResult.InvalidRange ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "periodEnd must not be before periodStart"))
        }

    @GetMapping("/top-category")
    fun topCategory(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam periodStart: LocalDate,
        @RequestParam periodEnd: LocalDate,
    ): ResponseEntity<*> =
        when (val result = expenseService.topCategory(currentUserProvider.resolve(userDetails), periodStart, periodEnd)) {
            is TopCategoryResult.Success -> ResponseEntity.ok(result.topCategory)
            is TopCategoryResult.NoData -> ResponseEntity.noContent().build<Unit>()
            is TopCategoryResult.InvalidRange ->
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "periodEnd must not be before periodStart"))
        }
}
