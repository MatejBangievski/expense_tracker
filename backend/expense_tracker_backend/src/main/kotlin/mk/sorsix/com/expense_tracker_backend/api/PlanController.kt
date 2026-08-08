package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.*
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreatePlanRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.PlanResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdatePlanRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.PlanService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plans")
class PlanController(
    private val planService: PlanService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun listPlans(@AuthenticationPrincipal userDetails: UserDetails): List<PlanResponse> =
        planService.listPlans(currentUserProvider.resolve(userDetails))

    @PostMapping
    fun createPlan(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: CreatePlanRequest
    ): ResponseEntity<*> =
        when (val result = planService.createPlan(currentUserProvider.resolve(userDetails), request)) {
            is CreatePlanResult.Success -> ResponseEntity.ok(result.plan)
            is CreatePlanResult.InvalidDateRange -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "End date cannot be before start date"))
        }

    @PutMapping("/{id}")
    fun updatePlan(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long,
        @RequestBody request: UpdatePlanRequest
    ): ResponseEntity<*> =
        when (val result = planService.updatePlan(currentUserProvider.resolve(userDetails), id, request)) {
            is UpdatePlanResult.Success -> ResponseEntity.ok(result.plan)
            is UpdatePlanResult.PlanNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Plan not found"))

            is UpdatePlanResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))

            is UpdatePlanResult.InvalidDateRange -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "End date cannot be before start date"))
        }

    @DeleteMapping("/{id}")
    fun deletePlan(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable id: Long
    ): ResponseEntity<*> =
        when (val result = planService.deletePlan(currentUserProvider.resolve(userDetails), id)) {
            is DeletePlanResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeletePlanResult.PlanNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Plan not found"))

            is DeletePlanResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))
        }
}