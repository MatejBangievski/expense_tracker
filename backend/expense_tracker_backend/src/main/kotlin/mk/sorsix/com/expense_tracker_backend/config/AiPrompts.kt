package mk.sorsix.com.expense_tracker_backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets


@Component
class AiPrompts(
    @Value("classpath:prompts/monthly-saving-recommendation.txt") recommendation: Resource,
    @Value("classpath:prompts/period-comparison.txt") comparison: Resource,
) {
    val monthlySavingRecommendation: String = recommendation.getContentAsString(StandardCharsets.UTF_8)
    val periodComparison: String = comparison.getContentAsString(StandardCharsets.UTF_8)
}