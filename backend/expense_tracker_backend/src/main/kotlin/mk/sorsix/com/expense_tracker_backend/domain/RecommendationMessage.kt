package mk.sorsix.com.expense_tracker_backend.domain

import jakarta.persistence.*


@Entity
data class RecommendationMessage(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne
    @JoinColumn(name = "user_id")
    val user: User = User(),

    @Column(columnDefinition = "TEXT")
    val message: String = "",

    val modelName: String? = null,

)