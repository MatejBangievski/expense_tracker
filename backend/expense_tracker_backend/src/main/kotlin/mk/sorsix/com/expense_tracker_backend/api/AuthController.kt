package mk.sorsix.com.expense_tracker_backend.api

import mk.sorsix.com.expense_tracker_backend.domain.LoginResult
import mk.sorsix.com.expense_tracker_backend.domain.LogoutResult
import mk.sorsix.com.expense_tracker_backend.domain.RefreshResult
import mk.sorsix.com.expense_tracker_backend.domain.RegisterResult
import mk.sorsix.com.expense_tracker_backend.domain.dto.AuthResponse
import mk.sorsix.com.expense_tracker_backend.domain.dto.LoginRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.RefreshRequest
import mk.sorsix.com.expense_tracker_backend.domain.dto.RegisterRequest
import mk.sorsix.com.expense_tracker_backend.service.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController (private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@RequestBody request : RegisterRequest): ResponseEntity<*> =
        when (val result = authService.register(request.displayName, request.email, request.password)) {
            is RegisterResult.Success -> ResponseEntity.ok(
                AuthResponse(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken
                )
            )
            is RegisterResult.EmailAlreadyInUse  -> ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("error" to "Email already in use."))
        }
    @PostMapping("/login")
    fun login(@RequestBody request : LoginRequest): ResponseEntity<*> =
        when(val result = authService.login(request.email, request.password)){
            is LoginResult.Success -> ResponseEntity.ok(
                AuthResponse(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken
                )
            )
            is LoginResult.InvalidCredentials -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid credentials."))
        }
    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): ResponseEntity<*> =
        when (val result = authService.refreshTokens(request.refreshToken)) {
            is RefreshResult.Success -> ResponseEntity.ok(AuthResponse(result.accessToken, result.refreshToken))
            is RefreshResult.InvalidToken -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid refresh token"))
            is RefreshResult.ExpiredOrRevoked -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Refresh token expired or revoked"))
        }

    @PostMapping("/logout")
    fun logout(@RequestBody request: RefreshRequest): ResponseEntity<*> =
        when (authService.logout(request.refreshToken)) {
            is LogoutResult.Success -> ResponseEntity.ok(mapOf("message" to "Logged out"))
            is LogoutResult.TokenNotFound -> ResponseEntity.ok(mapOf("message" to "Logged out"))
        //namerno isto bodu za da ne se otkrije dali e istecen tokenot ili dali ne postoi
        }
}