package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_centres")
data class SavedCentreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val category: String,
    val distanceKm: Double,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_tokens")
data class UserTokenEntity(
    @PrimaryKey val id: String,
    val tokenNumber: String,
    val centreId: String,
    val centreName: String,
    val serviceId: String,
    val serviceName: String,
    val status: String,
    val counterNumber: Int?,
    val positionInQueue: Int,
    val peopleAhead: Int,
    val estimatedWaitMin: Int,
    val estimatedWaitMax: Int,
    val joinedAtTimestamp: Long
)

@Entity(tableName = "crowd_reports_local")
data class OfflineCrowdReportEntity(
    @PrimaryKey val id: String,
    val centreId: String,
    val centreName: String,
    val crowdLevel: String,
    val approxPeopleCount: Int,
    val reportedWaitMinutes: Int,
    val notes: String,
    val timestamp: Long
)

@Entity(tableName = "centre_reviews")
data class CentreReviewEntity(
    @PrimaryKey val id: String,
    val centreId: String,
    val centreName: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val reviewText: String,
    val visitTag: String,
    val queueSpeedRating: String,
    val timestamp: Long = System.currentTimeMillis(),
    val helpfulCount: Int = 0
)

@Entity(tableName = "live_queue_updates")
data class LiveQueueUpdateEntity(
    @PrimaryKey val id: String,
    val centreId: String,
    val centreName: String,
    val reporterName: String,
    val updateTag: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val currentServingEstimate: String,
    val upvotes: Int = 0
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val assignedCentreId: String?,
    val assignedCounterId: String?,
    val pin: String,
    val department: String,
    val isActive: Boolean = true
)

@Entity(tableName = "service_change_proposals")
data class ServiceChangeProposalEntity(
    @PrimaryKey val id: String,
    val proposalType: String,
    val serviceId: String,
    val serviceTitle: String,
    val centreId: String,
    val submittedByStaffId: String,
    val submittedByStaffName: String,
    val assignedCounterNumber: Int,
    val rationale: String,
    val status: String,
    val submittedAt: Long,
    val reviewedAt: Long?,
    val reviewedByAdminName: String?,
    val adminReviewNote: String,
    val previousFee: String,
    val proposedFee: String,
    val previousDurationMin: Int,
    val proposedDurationMin: Int,
    val previousDescription: String,
    val proposedDescription: String,
    val previousEligibility: String,
    val proposedEligibility: String,
    val previousCautionNote: String,
    val proposedCautionNote: String,
    val documentId: String,
    val previousDocName: String,
    val proposedDocName: String,
    val previousDocDescription: String,
    val proposedDocDescription: String,
    val previousDocMandatory: Boolean,
    val proposedDocMandatory: Boolean,
    val previousDocAlternativesJoined: String,
    val proposedDocAlternativesJoined: String
)

@Entity(tableName = "staff_notifications")
data class StaffNotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val targetRole: String,
    val centreId: String?,
    val counterId: String?,
    val severity: String,
    val isRead: Boolean,
    val timestamp: Long,
    val relatedTokenNumber: String?
)

@Entity(tableName = "system_settings")
data class SystemSettingsEntity(
    @PrimaryKey val id: String,
    val autoNoShowTimeoutMinutes: Int,
    val maxActiveTokensPerCitizen: Int,
    val crowdDecayHalfLifeMinutes: Int,
    val walkInBufferPerHour: Int,
    val maintenanceMode: Boolean,
    val maintenanceBanner: String,
    val updatedAt: Long,
    val updatedByAdmin: String
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val action: String,
    val actorName: String,
    val actorRole: String,
    val details: String,
    val centreId: String?
)

