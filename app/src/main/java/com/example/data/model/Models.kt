package com.example.data.model

enum class UserRole {
    CUSTOMER,
    STAFF,
    ADMIN
}

enum class TokenStatus {
    WAITING,
    CALLED,
    SERVING,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}

enum class CrowdLevel(val label: String, val threshold: String) {
    LOW("Low Crowd", "1-10 people"),
    MODERATE("Moderate", "10-25 people"),
    HIGH("High Crowd", "25-50 people"),
    SEVERE("Severe / Packed", "50+ people")
}

enum class ServiceCategory(val displayName: String, val iconName: String) {
    CIVIL_ID("Civil & Identity", "badge"),
    HEALTHCARE("Hospitals & Healthcare", "local_hospital"),
    TRANSPORT_RTO("Transport & RTO", "directions_car"),
    BANKING_FINANCE("Banking & Treasury", "account_balance"),
    MUNICIPAL_CIVIC("Municipal & Civic Services", "location_city"),
    EDUCATION("Education & University", "school")
}

data class RequiredDocument(
    val id: String,
    val name: String,
    val description: String,
    val isMandatory: Boolean = true,
    val acceptableAlternatives: List<String> = emptyList(),
    val remarks: String = ""
)

data class ServiceItem(
    val id: String,
    val category: ServiceCategory,
    val title: String,
    val code: String,
    val description: String,
    val officialFee: String,
    val avgProcessingDurationMin: Int,
    val requiredDocuments: List<RequiredDocument>,
    val eligibility: String,
    val proceduralSteps: List<String>,
    val isAvailable: Boolean = true,
    val cautionNote: String = ""
)

data class Counter(
    val id: String,
    val centreId: String,
    val counterNumber: Int,
    val name: String,
    val assignedStaffName: String,
    val isActive: Boolean = true,
    val currentServingToken: String? = null,
    val handledServiceIds: List<String> = emptyList()
)

data class ServiceCentre(
    val id: String,
    val name: String,
    val category: ServiceCategory,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val operatingHours: String,
    val phone: String,
    val activeCountersCount: Int,
    val totalCountersCount: Int,
    val currentCrowdLevel: CrowdLevel,
    val estimatedWaitMin: Int,
    val estimatedWaitMax: Int,
    val lastCrowdUpdateMinutesAgo: Int,
    val crowdConfidencePercent: Int,
    val bestTimeToVisit: String,
    val isVirtualQueueOpen: Boolean = true,
    val availableServiceIds: List<String> = emptyList(),
    val rating: Double = 4.7,
    val totalReviewsCount: Int = 128,
    val liveNotice: String? = null
)

data class QueueToken(
    val id: String,
    val tokenNumber: String,
    val centreId: String,
    val centreName: String,
    val serviceId: String,
    val serviceName: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val status: TokenStatus,
    val counterNumber: Int? = null,
    val counterName: String? = null,
    val positionInQueue: Int,
    val peopleAhead: Int,
    val estimatedWaitMin: Int,
    val estimatedWaitMax: Int,
    val joinedAtTimestamp: Long = System.currentTimeMillis(),
    val calledAtTimestamp: Long? = null,
    val servingAtTimestamp: Long? = null,
    val completedAtTimestamp: Long? = null,
    val qrVerificationCode: String = "QEASE-TKN-$tokenNumber"
)

data class CrowdReport(
    val id: String,
    val centreId: String,
    val centreName: String,
    val crowdLevel: CrowdLevel,
    val approxPeopleCount: Int,
    val reportedWaitMinutes: Int,
    val notes: String,
    val reporterName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotesCount: Int = 1,
    val isStaffVerified: Boolean = false,
    val confidenceScore: Int = 90
)

data class QueueEvent(
    val id: String,
    val tokenId: String,
    val tokenNumber: String,
    val centreId: String,
    val fromStatus: TokenStatus,
    val toStatus: TokenStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val actorRole: UserRole,
    val actorName: String,
    val counterId: String? = null,
    val reason: String = ""
)

data class AuditLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val actorName: String,
    val actorRole: UserRole,
    val details: String,
    val centreId: String? = null
)

data class KnowledgeDocument(
    val id: String,
    val serviceId: String?,
    val title: String,
    val category: ServiceCategory,
    val content: String,
    val officialAuthority: String,
    val verifiedDate: String,
    val keywords: List<String>
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val groundedSources: List<String> = emptyList(),
    val suggestedServiceId: String? = null
)

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val assignedCentreId: String? = null,
    val assignedCounterId: String? = null,
    val pin: String = "",
    val department: String = "",
    val isLoggedIn: Boolean = true
)

data class CentreReview(
    val id: String,
    val centreId: String,
    val centreName: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val reviewText: String,
    val visitTag: String,
    val queueSpeedRating: String, // "Fast", "Normal", "Slow"
    val timestamp: Long = System.currentTimeMillis(),
    val helpfulCount: Int = 0,
    val isVerifiedVisitor: Boolean = true
)

data class LiveQueueStatusUpdate(
    val id: String,
    val centreId: String,
    val centreName: String,
    val reporterName: String,
    val updateTag: String, // "FAST_MOVING", "SLIGHT_DELAY", "DOCS_ACTIVE", "COUNTER_ADDED", "ON_TRACK"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val currentServingEstimate: String,
    val upvotes: Int = 2
)
