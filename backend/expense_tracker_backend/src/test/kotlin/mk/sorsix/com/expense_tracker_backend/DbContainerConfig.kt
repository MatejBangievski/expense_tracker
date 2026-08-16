package mk.sorsix.com.expense_tracker_backend

import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName


object DbContainerConfig {
    val postgres: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("expenses_tracker_test")
            .withUsername("test")
            .withPassword("test")
            .apply { start() }
    }
}