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

enum class AiActionType {
    OPEN_SERVICE_CHECKLIST,
    PREFILL_STAFF_SERVICE_PROPOSAL,
    PREFILL_STAFF_DOC_PROPOSAL,
    OPEN_STAFF_ALERTS,
    VERIFY_LATEST_CROWD_REPORT,
    OPEN_STAFF_DESK,
    OPEN_ADMIN_APPROVALS,
    OPEN_ADMIN_OPS_STAFF,
    OPEN_ADMIN_OPS_CENTRES,
    OPEN_ADMIN_OPS_SETTINGS,
    OPEN_ADMIN_ANALYTICS
}

data class AiWorkflowAction(
    val type: AiActionType,
    val label: String,
    val targetServiceId: String? = null,
    val targetProposalId: String? = null,
    val targetCentreId: String? = null,
    val targetReportId: String? = null,
    val prefillFee: String? = null,
    val prefillDurationMin: Int? = null,
    val prefillCautionNote: String? = null,
    val prefillRationale: String? = null,
    val prefillDocName: String? = null,
    val prefillDocDescription: String? = null
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val groundedSources: List<String> = emptyList(),
    val suggestedServiceId: String? = null,
    val workflowAction: AiWorkflowAction? = null
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
    val isLoggedIn: Boolean = true,
    val isActive: Boolean = true
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

enum class ProposalType(val label: String) {
    SERVICE_INFO("Service Info Edit"),
    REQUIRED_DOCUMENT_ADD("Add Required Doc"),
    REQUIRED_DOCUMENT_UPDATE("Edit Required Doc"),
    REQUIRED_DOCUMENT_REMOVE("Remove Required Doc")
}

enum class ProposalStatus(val label: String) {
    PENDING("Pending Review"),
    APPROVED("Approved & Live"),
    REJECTED("Rejected")
}

data class ServiceChangeProposal(
    val id: String,
    val proposalType: ProposalType,
    val serviceId: String,
    val serviceTitle: String,
    val centreId: String,
    val submittedByStaffId: String,
    val submittedByStaffName: String,
    val assignedCounterNumber: Int,
    val rationale: String,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedByAdminName: String? = null,
    val adminReviewNote: String = "",
    // Service Info fields
    val previousFee: String = "",
    val proposedFee: String = "",
    val previousDurationMin: Int = 0,
    val proposedDurationMin: Int = 0,
    val previousDescription: String = "",
    val proposedDescription: String = "",
    val previousEligibility: String = "",
    val proposedEligibility: String = "",
    val previousCautionNote: String = "",
    val proposedCautionNote: String = "",
    // Document Proposal fields
    val documentId: String = "",
    val previousDocName: String = "",
    val proposedDocName: String = "",
    val previousDocDescription: String = "",
    val proposedDocDescription: String = "",
    val previousDocMandatory: Boolean = true,
    val proposedDocMandatory: Boolean = true,
    val previousDocAlternatives: List<String> = emptyList(),
    val proposedDocAlternatives: List<String> = emptyList()
)

enum class NotificationSeverity(val label: String) {
    INFO("Queue Update"),
    ACTION_REQUIRED("Action Required"),
    APPROVED("Approved"),
    ALERT("Alert")
}

data class StaffNotification(
    val id: String,
    val title: String,
    val message: String,
    val targetRole: UserRole = UserRole.STAFF,
    val centreId: String? = null,
    val counterId: String? = null,
    val severity: NotificationSeverity = NotificationSeverity.INFO,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedTokenNumber: String? = null
)

data class SystemSettings(
    val id: String = "global",
    val autoNoShowTimeoutMinutes: Int = 15,
    val maxActiveTokensPerCitizen: Int = 2,
    val crowdDecayHalfLifeMinutes: Int = 20,
    val walkInBufferPerHour: Int = 4,
    val maintenanceMode: Boolean = false,
    val maintenanceBanner: String = "Scheduled e-Governance maintenance window: 02:00 AM – 02:30 AM",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedByAdmin: String = "System Administrator"
)

data class HourlyTrendPoint(
    val hourLabel: String,
    val tokenCount: Int,
    val avgWaitMinutes: Int
)

data class QueueStatisticsSnapshot(
    val totalTokensToday: Int,
    val waitingCount: Int,
    val activeServingCount: Int,
    val completedCount: Int,
    val noShowCount: Int,
    val cancelledCount: Int,
    val completionRatePercent: Int,
    val avgWaitMinutes: Int,
    val activeCountersCount: Int,
    val totalCountersCount: Int,
    val pendingProposalsCount: Int,
    val hourlyTrends: List<HourlyTrendPoint>
)

