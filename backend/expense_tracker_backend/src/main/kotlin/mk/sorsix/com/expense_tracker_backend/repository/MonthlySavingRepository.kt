package mk.sorsix.com.expense_tracker_backend.repository

import mk.sorsix.com.expense_tracker_backend.domain.MonthlySaving
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface MonthlySavingRepository : JpaRepository<MonthlySaving, Long> {
    fun findByUserIdAndSavingMonth(userId: Long, savingMonth: LocalDate): MonthlySaving?
}