package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.CreatePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.DeletePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.UpdatePlanItemResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.CreatePlanItemRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdatePlanItemRequest
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.PlanItemService
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
@RequestMapping("/api/plans/{planId}/items")
class PlanItemController(
    private val planItemService: PlanItemService,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping
    fun listPlanItems(
        @PathVariable planId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<*> {
        val items = planItemService.listPlanItems(currentUserProvider.resolve(userDetails), planId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Plan not found"))
        return ResponseEntity.ok(items)
    }

    @PostMapping
    fun createPlanItem(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @RequestBody request: CreatePlanItemRequest
    ): ResponseEntity<*> =
        when (val result = planItemService.createPlanItem(currentUserProvider.resolve(userDetails), planId, request)) {
            is CreatePlanItemResult.Success ->
                ResponseEntity.ok(result.item)

            is CreatePlanItemResult.PlanNotFound ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Plan not found"))

            is CreatePlanItemResult.NotOwner ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to "You do not own this plan"))

            is CreatePlanItemResult.CategoryNotFound ->
                ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to "Category not found"))
        }

    @PutMapping("/{itemId}")
    fun update(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @PathVariable itemId: Long,
        @RequestBody request: UpdatePlanItemRequest
    ): ResponseEntity<*> =
        when (val result = planItemService.updatePlanItem(currentUserProvider.resolve(userDetails), itemId, request)) {
            is UpdatePlanItemResult.Success -> ResponseEntity.ok(result.item)
            is UpdatePlanItemResult.ItemNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Plan item not found"))

            is UpdatePlanItemResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))

            is UpdatePlanItemResult.CategoryNotFound -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Category not found"))
        }

    @DeleteMapping("/{itemId}")
    fun delete(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable planId: Long,
        @PathVariable itemId: Long
    ): ResponseEntity<*> =
        when (val result = planItemService.deletePlanItem(currentUserProvider.resolve(userDetails), itemId)) {
            is DeletePlanItemResult.Success -> ResponseEntity.noContent().build<Unit>()
            is DeletePlanItemResult.ItemNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "Plan item not found"))

            is DeletePlanItemResult.NotOwner -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "You do not own this plan"))
        }
}