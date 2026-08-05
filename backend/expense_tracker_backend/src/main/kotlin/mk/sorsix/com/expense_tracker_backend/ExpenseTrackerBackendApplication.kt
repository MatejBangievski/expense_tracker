package mk.sorsix.com.expense_tracker_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExpenseTrackerBackendApplication

fun main(args: Array<String>) {
    runApplication<ExpenseTrackerBackendApplication>(*args)
}
