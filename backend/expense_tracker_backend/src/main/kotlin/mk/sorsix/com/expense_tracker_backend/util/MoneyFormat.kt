package mk.sorsix.com.expense_tracker_backend.util

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.money(): String = setScale(2, RoundingMode.HALF_UP).toPlainString()
