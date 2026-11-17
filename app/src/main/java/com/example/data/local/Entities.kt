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
    val department: String
)
