package mk.sorsix.com.expense_tracker_backend.service

import mk.sorsix.com.expense_tracker_backend.domain.IssuedTokens
import mk.sorsix.com.expense_tracker_backend.domain.LoginResult
import mk.sorsix.com.expense_tracker_backend.domain.LogoutResult
import mk.sorsix.com.expense_tracker_backend.domain.RefreshResult
import mk.sorsix.com.expense_tracker_backend.domain.RefreshToken
import mk.sorsix.com.expense_tracker_backend.domain.RegisterResult
import mk.sorsix.com.expense_tracker_backend.domain.User
import mk.sorsix.com.expense_tracker_backend.repository.RefreshTokenRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import mk.sorsix.com.expense_tracker_backend.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    fun register(
        displayName: String,
        email: String,
        password: String,
    ): RegisterResult {
        if (userRepository.findByEmail(email) != null) {
            return RegisterResult.EmailAlreadyInUse
        }
        // encode() е декларирана како @Nullable, но според @Contract анотацијата
        // (!null -> !null), таа секогаш враќа non-null резултат кога влезниот
        // параметар не е null. Бидејќи 'password' овде е гарантирано non-null
        // String, безбедно е да се потврди дека резултатот нема да биде null.
        val user =
            userRepository.save(
                User(
                    displayName = displayName,
                    email = email,
                    passwordHash = passwordEncoder.encode(password)!!,
                ),
            )
        val tokens = issueTokens(user)
        return RegisterResult.Success(tokens.accessToken, tokens.refreshToken)
    }

    fun login(
        email: String,
        password: String,
    ): LoginResult {
        val user = userRepository.findByEmail(email) ?: return LoginResult.InvalidCredentials
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            return LoginResult.InvalidCredentials
        }
        val tokens = issueTokens(user)
        return LoginResult.Success(tokens.accessToken, tokens.refreshToken)
    }

    fun refreshTokens(refreshTokenValue: String): RefreshResult {
        val storedToken = refreshTokenRepository.findByToken(refreshTokenValue) ?: return RefreshResult.InvalidToken
        if (storedToken.revoked || storedToken.expiresAt.isBefore(Instant.now())) {
            return RefreshResult.ExpiredOrRevoked
        }
        refreshTokenRepository.save(storedToken.copy(revoked = true))
        val tokens = issueTokens(storedToken.user)
        return RefreshResult.Success(tokens.accessToken, tokens.refreshToken)
    }

    fun logout(refreshTokenValue: String): LogoutResult {
        val storedToken =
            refreshTokenRepository.findByToken(refreshTokenValue)
                ?: return LogoutResult.TokenNotFound

        refreshTokenRepository.save(storedToken.copy(revoked = true))
        return LogoutResult.Success
    }

    private fun issueTokens(user: User): IssuedTokens {
        val accessToken = jwtService.generateAccessToken(user.email)
        val refreshTokenValue = jwtService.generateRefreshToken()

        refreshTokenRepository.save(
            RefreshToken(user = user, token = refreshTokenValue, expiresAt = jwtService.refreshTokenExpiryInstant()),
        )

        return IssuedTokens(accessToken, refreshTokenValue)
    }
}
