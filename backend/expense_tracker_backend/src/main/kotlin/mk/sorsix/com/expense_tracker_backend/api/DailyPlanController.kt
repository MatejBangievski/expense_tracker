package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.*
import mk.sorsix.com.expense_tracker_backend.domain.dto.DailyPlanRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.DailyPlanService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plans/{planId}/daily")
class DailyPlanController(
    private val dailyPlanService: DailyPlanService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long
    ): ResponseEntity<*> {
        val items = dailyPlanService.listDailyPlans(currentUserProvider.resolve(userDetails), planId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Plan not found"))
        return ResponseEntity.ok(items)
    }

    @PostMapping
    fun create(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @RequestBody request: DailyPlanRequest
    ): ResponseEntity<*> =
        when (val result =
            dailyPlanService.createDailyPlan(currentUserProvider.resolve(userDetails), planId, request)) {
            is CreateDailyPlanResult.Success -> ResponseEntity.ok(result.dailyPlan)
            is CreateDailyPlanResult.PlanNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Plan not found"))

            is CreateDailyPlanResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))

            is CreateDailyPlanResult.DateOutsideRange -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Date is outside the plan's date range"))

            is CreateDailyPlanResult.AlreadyExists -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "An allocation already exists for this date"))

            is CreateDailyPlanResult.OverBudgetWarning -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "over_budget", "remainingBudget" to result.remainingBudget))
        }

    @PutMapping("/{dailyPlanId}")
    fun update(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @PathVariable dailyPlanId: Long,
        @RequestBody request: DailyPlanRequest
    ): ResponseEntity<*> =
        when (val result =
            dailyPlanService.updateDailyPlan(currentUserProvider.resolve(userDetails), dailyPlanId, request)) {
            is UpdateDailyPlanResult.Success -> ResponseEntity.ok(result.dailyPlan)
            is UpdateDailyPlanResult.DailyPlanNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Daily allocation not found"))

            is UpdateDailyPlanResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))
        }

    @DeleteMapping("/{dailyPlanId}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @PathVariable dailyPlanId: Long
    ): ResponseEntity<*> =
        when (val result = dailyPlanService.deleteDailyPlan(currentUserProvider.resolve(userDetails), dailyPlanId)) {
            is DeleteDailyPlanResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeleteDailyPlanResult.DailyPlanNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Daily allocation not found"))

            is DeleteDailyPlanResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))
        }
}