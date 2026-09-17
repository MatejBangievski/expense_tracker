package mk.sorsix.com.expense_tracker_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class ClockConfig {
    @Bean
    fun clock(
        @Value("\${app.scheduling.zone:Europe/Skopje}") zone: String,
    ): Clock = Clock.system(ZoneId.of(zone))
}
