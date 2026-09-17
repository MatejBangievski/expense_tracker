package mk.sorsix.com.expense_tracker_backend.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secretString: String,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secretString.toByteArray())

    private val accessTokenExpirationMillis = 1000 * 60 * 15
    private val refreshExpirationMillis = 1000L * 60 * 60 * 24 * 7

    fun generateAccessToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirationMillis)
        return Jwts
            .builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun refreshTokenExpiryInstant(): Instant = Instant.now().plusMillis(refreshExpirationMillis)

    fun validateAccessToken(refreshToken: String): TokenValidationResult {
        val email =
            try {
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .payload.subject
            } catch (e: Exception) {
                null
            }
        return if (email != null) {
            TokenValidationResult.Valid(email)
        } else {
            TokenValidationResult.Invalid
        }
    }
}
