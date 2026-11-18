package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QEaseDao {

    // Saved Centres
    @Query("SELECT * FROM saved_centres ORDER BY savedAtTimestamp DESC")
    fun getAllSavedCentres(): Flow<List<SavedCentreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCentre(centre: SavedCentreEntity)

    @Query("DELETE FROM saved_centres WHERE id = :centreId")
    suspend fun deleteSavedCentre(centreId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_centres WHERE id = :centreId)")
    fun isCentreSaved(centreId: String): Flow<Boolean>

    // User Tokens
    @Query("SELECT * FROM user_tokens ORDER BY joinedAtTimestamp DESC")
    fun getAllUserTokens(): Flow<List<UserTokenEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertToken(token: UserTokenEntity)

    @Query("DELETE FROM user_tokens WHERE id = :tokenId")
    suspend fun deleteToken(tokenId: String)

    // Crowd Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrowdReport(report: OfflineCrowdReportEntity)

    @Query("SELECT * FROM crowd_reports_local ORDER BY timestamp DESC")
    fun getAllLocalReports(): Flow<List<OfflineCrowdReportEntity>>

    // Centre Reviews
    @Query("SELECT * FROM centre_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<CentreReviewEntity>>

    @Query("SELECT * FROM centre_reviews WHERE centreId = :centreId ORDER BY timestamp DESC")
    fun getReviewsForCentre(centreId: String): Flow<List<CentreReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CentreReviewEntity)

    @Query("UPDATE centre_reviews SET helpfulCount = helpfulCount + 1 WHERE id = :reviewId")
    suspend fun upvoteReview(reviewId: String)

    // Live Queue Status / "Where is my train" updates
    @Query("SELECT * FROM live_queue_updates ORDER BY timestamp DESC")
    fun getAllLiveQueueUpdates(): Flow<List<LiveQueueUpdateEntity>>

    @Query("SELECT * FROM live_queue_updates WHERE centreId = :centreId ORDER BY timestamp DESC")
    fun getLiveQueueUpdatesForCentre(centreId: String): Flow<List<LiveQueueUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLiveQueueUpdate(update: LiveQueueUpdateEntity)

    @Query("UPDATE live_queue_updates SET upvotes = upvotes + 1 WHERE id = :updateId")
    suspend fun upvoteLiveUpdate(updateId: String)

    // User Accounts & RBAC
    @Query("SELECT * FROM user_accounts")
    fun getAllAccounts(): Flow<List<UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: UserAccountEntity)

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccountEntity?

    // Service Change Proposals (Staff -> Admin approval gate)
    @Query("SELECT * FROM service_change_proposals ORDER BY submittedAt DESC")
    fun getAllProposals(): Flow<List<ServiceChangeProposalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProposal(proposal: ServiceChangeProposalEntity)

    // Staff & Admin Notifications
    @Query("SELECT * FROM staff_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<StaffNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotification(notification: StaffNotificationEntity)

    @Query("UPDATE staff_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationRead(notificationId: String)

    // System Settings
    @Query("SELECT * FROM system_settings WHERE id = 'global' LIMIT 1")
    fun observeGlobalSettings(): Flow<SystemSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSystemSettings(settings: SystemSettingsEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

