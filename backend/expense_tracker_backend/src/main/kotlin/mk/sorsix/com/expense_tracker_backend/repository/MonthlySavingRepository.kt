package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.MonthlySaving
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDate

interface MonthlySavingRepository : JpaRepository<MonthlySaving, Long> {
    fun findByUserIdAndSavingMonth(userId: Long, savingMonth: LocalDate): MonthlySaving?

    @Query(
        "SELECT COALESCE(SUM(m.totalSaved), 0) FROM MonthlySaving m " +
            "WHERE m.user.id = :userId AND m.savingMonth < :beforeMonth AND m.totalSaved > 0"
    )
    fun sumTotalSavedBefore(@Param("userId") userId: Long, @Param("beforeMonth") beforeMonth: LocalDate): BigDecimal
}