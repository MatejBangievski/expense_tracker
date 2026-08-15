package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.dto.ApiKeyResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.SetApiKeyRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.UpdateUserResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.UserResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.ValidateApiKeyRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.ValidateApiKeyResponse
import mk.sorsix.com.expense_tracker_backend.security.CurrentUserProvider
import mk.sorsix.com.expense_tracker_backend.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me")
class UserController(
    private val userService: UserService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userDetails: UserDetails): UserResponse =
        userService.getProfile(currentUserProvider.resolve(userDetails))

    @PutMapping
    fun updateProfile(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<*> =
        when (val result = userService.updateProfile(currentUserProvider.resolve(userDetails), request)) {
            is UpdateUserResult.Success -> ResponseEntity.ok(result.user)
            is UpdateUserResult.UserNotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to "User not found"))
        }

    @PutMapping("/api-key")
    fun setApiKey(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: SetApiKeyRequest,
    ): ResponseEntity<ApiKeyResponse> =
        ResponseEntity.ok(ApiKeyResponse(userService.setApiKey(currentUserProvider.resolve(userDetails), request.apiKey)))

    @GetMapping("/api-key")
    fun getApiKey(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestParam(defaultValue = "true") masked: Boolean,
    ): ResponseEntity<ApiKeyResponse> =
        ResponseEntity.ok(ApiKeyResponse(userService.getApiKey(currentUserProvider.resolve(userDetails), masked)))

    @DeleteMapping("/api-key")
    fun clearApiKey(@AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<Void> {
        userService.clearApiKey(currentUserProvider.resolve(userDetails))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api-key/validate")
    fun validateApiKey(@RequestBody request: ValidateApiKeyRequest): ResponseEntity<ValidateApiKeyResponse> =
        ResponseEntity.ok(ValidateApiKeyResponse(userService.validateApiKey(request.apiKey)))
}