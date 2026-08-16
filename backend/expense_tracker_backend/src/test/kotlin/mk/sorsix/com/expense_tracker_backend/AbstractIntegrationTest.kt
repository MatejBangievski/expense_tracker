package mk.sorsix.com.expense_tracker_backend

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import javax.sql.DataSource


abstract class AbstractIntegrationTest {

    @Autowired
    private lateinit var dataSource: DataSource

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { DbContainerConfig.postgres.jdbcUrl }
            registry.add("spring.datasource.username") { DbContainerConfig.postgres.username }
            registry.add("spring.datasource.password") { DbContainerConfig.postgres.password }
        }

        private val CLEANUP_STATEMENTS = listOf(
            "DELETE FROM budget",
            "DELETE FROM period_comparison",
            "DELETE FROM period_summary_category",
            "DELETE FROM period_summary",
            "DELETE FROM monthly_saving",
            "DELETE FROM expense",
            "DELETE FROM daily_plan",
            "DELETE FROM plan_item",
            "DELETE FROM plan",
            "DELETE FROM refresh_token",
            "DELETE FROM category WHERE user_id IS NOT NULL",
            "DELETE FROM app_user",
        )
    }

    @BeforeEach
    fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                CLEANUP_STATEMENTS.forEach { stmt.execute(it) }
            }
        }
    }
}