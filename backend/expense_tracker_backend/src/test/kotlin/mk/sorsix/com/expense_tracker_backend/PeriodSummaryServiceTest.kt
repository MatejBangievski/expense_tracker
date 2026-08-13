package mk.sorsix.com.expense_tracker_backend.api

import com.fasterxml.jackson.databind.ObjectMapper
import mk.sorsix.com.expense_tracker_backend.TestUser
import mk.sorsix.com.expense_tracker_backend.domain.GeneratePeriodSummaryResult
import mk.sorsix.com.expense_tracker_backend.domain.PeriodType
import mk.sorsix.com.expense_tracker_backend.domain.dto.PeriodSummaryResponse
import mk.sorsix.com.expense_tracker_backend.registerAndLogin
import mk.sorsix.com.expense_tracker_backend.repository.PeriodSummaryCategoryRepository
import mk.sorsix.com.expense_tracker_backend.repository.UserRepository
import mk.sorsix.com.expense_tracker_backend.service.PeriodSummaryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate


@SpringBootTest
@AutoConfigureMockMvc
class PeriodSummaryServiceTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var periodSummaryService: PeriodSummaryService
    @Autowired lateinit var periodSummaryCategoryRepository: PeriodSummaryCategoryRepository
    @Autowired lateinit var userRepository: UserRepository

    private val july: LocalDate = LocalDate.of(2026, 7, 1)

    private fun seedUser(vararg expenses: Triple<Long, String, String>): Pair<TestUser, Long> {
        val user = mockMvc.registerAndLogin(objectMapper)
        expenses.forEach { (categoryId, amount, date) ->
            mockMvc.post("/api/expenses") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer ${user.accessToken}")
                content = """{"categoryId":$categoryId,"amount":$amount,"expenseDate":"$date"}"""
            }
        }
        return user to userRepository.findByEmail(user.email)!!.id
    }

    private fun generate(userId: Long, periodType: PeriodType = PeriodType.MONTH, date: LocalDate = july): PeriodSummaryResponse {
        val result = periodSummaryService.generateForUser(userId, periodType, date)
        assertThat(result).isInstanceOf(GeneratePeriodSummaryResult.Success::class.java)
        return (result as GeneratePeriodSummaryResult.Success).summary
    }

    @Test
    fun `total spent is the sum of the months expenses`() {
        val (_, userId) = seedUser(
            Triple(5L, "120.55", "2026-07-03"),
            Triple(6L, "42.00", "2026-07-18"),
            Triple(7L, "45.00", "2026-07-09"),
        )

        assertThat(generate(userId).totalSpent).isEqualByComparingTo(BigDecimal("207.55"))
    }

    @Test
    fun `leaf categories roll up under their root and totals reconcile`() {
        val (_, userId) = seedUser(
            Triple(5L, "120.55", "2026-07-03"),
            Triple(6L, "42.00", "2026-07-18"),
            Triple(7L, "45.00", "2026-07-09"),
        )

        val summary = generate(userId)

        val food = summary.categories.single { it.categoryName == "Food" }
        assertThat(food.totalAmount).isEqualByComparingTo(BigDecimal("162.55"))
        assertThat(food.expenseCount).isEqualTo(2)
        assertThat(food.subcategories.map { it.categoryName })
            .containsExactlyInAnyOrder("Groceries", "Dining Out")
        assertThat(food.subcategories.fold(BigDecimal.ZERO) { acc, s -> acc + s.totalAmount })
            .isEqualByComparingTo(food.totalAmount)

        val transportation = summary.categories.single { it.categoryName == "Transportation" }
        assertThat(transportation.totalAmount).isEqualByComparingTo(BigDecimal("45.00"))

        assertThat(summary.categories.fold(BigDecimal.ZERO) { acc, c -> acc + c.totalAmount })
            .isEqualByComparingTo(summary.totalSpent)
    }

    @Test
    fun `expenses outside the month are excluded`() {
        val (_, userId) = seedUser(
            Triple(5L, "100.00", "2026-07-15"),
            Triple(5L, "999.00", "2026-08-15"),
            Triple(5L, "888.00", "2026-06-30"),
        )

        assertThat(generate(userId).totalSpent).isEqualByComparingTo(BigDecimal("100.00"))
    }

    @Test
    fun `regenerating keeps the same summary row and does not duplicate categories`() {
        val (_, userId) = seedUser(
            Triple(5L, "120.55", "2026-07-03"),
            Triple(6L, "42.00", "2026-07-18"),
        )

        val first = generate(userId)
        val second = generate(userId)

        assertThat(second.summaryId).isEqualTo(first.summaryId)
        assertThat(periodSummaryCategoryRepository.findAllWithCategoryBySummaryId(first.summaryId))
            .hasSize(2)
        assertThat(second.totalSpent).isEqualByComparingTo(first.totalSpent)
    }

    @Test
    fun `a month with no expenses yields a zero summary`() {
        val (_, userId) = seedUser()

        val summary = generate(userId)

        assertThat(summary.totalSpent).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(summary.categories).isEmpty()
    }

    @Test
    fun `a weekly summary only counts the ISO week and normalizes to its Monday`() {
        // ISO week Mon 2026-07-06 .. Sun 2026-07-12
        val (_, userId) = seedUser(
            Triple(5L, "30.00", "2026-07-07"), // Tuesday, in week
            Triple(6L, "20.00", "2026-07-08"), // Wednesday, in week
            Triple(5L, "999.00", "2026-07-01"), // previous week, excluded
            Triple(5L, "888.00", "2026-07-13"), // next week (Monday), excluded
        )

        val summary = generate(userId, PeriodType.WEEK, LocalDate.of(2026, 7, 7))

        assertThat(summary.periodType).isEqualTo(PeriodType.WEEK)
        assertThat(summary.periodStart).isEqualTo(LocalDate.of(2026, 7, 6))
        assertThat(summary.totalSpent).isEqualByComparingTo(BigDecimal("50.00"))
    }

    @Test
    fun `a yearly summary counts the whole calendar year and normalizes to Jan 1`() {
        val (_, userId) = seedUser(
            Triple(5L, "100.00", "2026-02-15"),
            Triple(6L, "50.00", "2026-11-30"),
            Triple(5L, "777.00", "2025-12-31"), // previous year, excluded
            Triple(5L, "666.00", "2027-01-01"), // next year, excluded
        )

        val summary = generate(userId, PeriodType.YEAR, LocalDate.of(2026, 6, 1))

        assertThat(summary.periodType).isEqualTo(PeriodType.YEAR)
        assertThat(summary.periodStart).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(summary.totalSpent).isEqualByComparingTo(BigDecimal("150.00"))
    }

    @Test
    fun `unknown user is reported rather than throwing`() {
        assertThat(periodSummaryService.generateForUser(999999, PeriodType.MONTH, july))
            .isEqualTo(GeneratePeriodSummaryResult.UserNotFound)
    }
}