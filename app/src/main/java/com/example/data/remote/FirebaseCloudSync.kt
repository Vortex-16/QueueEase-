package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseCloudSync(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    interface SyncCallbacks {
        fun onServicesUpdated(items: List<ServiceItem>)
        fun onCentresUpdated(items: List<ServiceCentre>)
        fun onCountersUpdated(items: List<Counter>)
        fun onTokensUpdated(items: List<QueueToken>, isFullCollection: Boolean)
        fun onStaffUpdated(items: List<UserAccount>)
        fun onProposalsUpdated(items: List<ServiceChangeProposal>)
        fun onNotificationsUpdated(items: List<StaffNotification>)
        fun onSettingsUpdated(settings: SystemSettings)
        fun onCrowdReportsUpdated(items: List<CrowdReport>)
        fun onReviewsUpdated(items: List<CentreReview>)
        fun onLiveUpdatesUpdated(items: List<LiveQueueStatusUpdate>)
        fun onAuditLogsUpdated(items: List<AuditLog>)
        fun seedSnapshotProvider(): SeedSnapshot
    }

    data class SeedSnapshot(
        val services: List<ServiceItem>,
        val centres: List<ServiceCentre>,
        val counters: List<Counter>,
        val staffAccounts: List<UserAccount>,
        val proposals: List<ServiceChangeProposal>,
        val notifications: List<StaffNotification>,
        val settings: SystemSettings,
        val crowdReports: List<CrowdReport>,
        val reviews: List<CentreReview>,
        val liveUpdates: List<LiveQueueStatusUpdate>
    )

    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableListOf<Job>()
    private var activeUid: String? = null
    private var activeRole: UserRole? = null

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun startSyncForSession(role: UserRole, callbacks: SyncCallbacks) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        if (uid.isBlank()) return
        if (activeUid == uid && activeRole == role && activeJobs.isNotEmpty()) return

        stopSync()
        activeUid = uid
        activeRole = role

        syncScope.launch {
            runCatching {
                ensureUserDocument(uid, user.displayName ?: "QEase User", user.email ?: "", role)
                val isAdminUser = ensureAdminIfBootstrapped(uid, user.email, user.isEmailVerified)
                if (role == UserRole.STAFF && !isAdminUser) {
                    claimStaffAccessIfEligible(uid, user.email, user.isEmailVerified)
                }
                if (isAdminUser) {
                    seedCatalogIfEmpty(uid, callbacks.seedSnapshotProvider())
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                Log.w(TAG, "Cloud session bootstrap note: ${err.message}")
            }

            attachRealtimeListeners(uid, user.email, role, callbacks)
        }
    }

    fun stopSync() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        activeUid = null
        activeRole = null
    }

    suspend fun clearOnSignOut() = withContext(NonCancellable) {
        stopSync()
    }

    private fun <T> observeSafe(
        path: String,
        flow: Flow<List<T>>,
        onUpdate: (List<T>) -> Unit
    ) {
        val boundUid = activeUid ?: return
        val job = syncScope.launch {
            flow.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == boundUid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    _lastSyncError.value = exceptionToLog.localizedMessage
                }
            }.collect { list ->
                if (list.isNotEmpty()) onUpdate(list)
            }
        }
        activeJobs.add(job)
    }

    private fun attachRealtimeListeners(
        uid: String,
        email: String?,
        role: UserRole,
        callbacks: SyncCallbacks
    ) {
        observeSafe(
            "centres",
            db.collection("centres").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toServiceCentreOrNull() } },
            callbacks::onCentresUpdated
        )
        observeSafe(
            "services",
            db.collection("services").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toServiceItemOrNull() } },
            callbacks::onServicesUpdated
        )
        observeSafe(
            "counters",
            db.collection("counters").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toCounterOrNull() } },
            callbacks::onCountersUpdated
        )
        observeSafe(
            "system_settings",
            db.collection("system_settings").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toSystemSettingsOrNull() } }
        ) { list ->
            list.firstOrNull()?.let(callbacks::onSettingsUpdated)
        }
        observeSafe(
            "crowd_reports",
            db.collection("crowd_reports").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toCrowdReportOrNull() } },
            callbacks::onCrowdReportsUpdated
        )
        observeSafe(
            "reviews",
            db.collection("reviews").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toCentreReviewOrNull() } },
            callbacks::onReviewsUpdated
        )
        observeSafe(
            "live_queue_updates",
            db.collection("live_queue_updates").whereEqualTo("visibility", "PUBLIC").snapshots()
                .map { snap -> snap.documents.mapNotNull { it.toLiveUpdateOrNull() } },
            callbacks::onLiveUpdatesUpdated
        )

        val isStaffOrAdmin = role == UserRole.STAFF || role == UserRole.ADMIN
        val tokensQuery: Query = if (isStaffOrAdmin) {
            db.collection("tokens")
        } else {
            db.collection("tokens").whereEqualTo("userId", uid)
        }
        observeSafe(
            "tokens",
            tokensQuery.snapshots().map { snap -> snap.documents.mapNotNull { it.toQueueTokenOrNull() } }
        ) { list -> callbacks.onTokensUpdated(list, isStaffOrAdmin) }

        if (role == UserRole.ADMIN || role == UserRole.STAFF) {
            val proposalQuery: Query = if (role == UserRole.ADMIN) {
                db.collection("service_change_proposals")
            } else {
                db.collection("service_change_proposals").whereEqualTo("submittedByUid", uid)
            }
            observeSafe(
                "service_change_proposals",
                proposalQuery.snapshots().map { snap -> snap.documents.mapNotNull { it.toProposalOrNull() } },
                callbacks::onProposalsUpdated
            )
        }

        val notifQuery: Query = when (role) {
            UserRole.ADMIN -> db.collection("notifications")
            UserRole.STAFF -> db.collection("notifications").whereEqualTo("targetRole", "STAFF")
            UserRole.CUSTOMER -> db.collection("notifications").whereEqualTo("recipientUid", uid)
        }
        observeSafe(
            "notifications",
            notifQuery.snapshots().map { snap -> snap.documents.mapNotNull { it.toNotificationOrNull() } },
            callbacks::onNotificationsUpdated
        )

        val auditQuery: Query = if (role == UserRole.ADMIN) {
            db.collection("audit_logs")
        } else {
            db.collection("audit_logs").whereEqualTo("actorUid", uid)
        }
        observeSafe(
            "audit_logs",
            auditQuery.snapshots().map { snap -> snap.documents.mapNotNull { it.toAuditLogOrNull() } },
            callbacks::onAuditLogsUpdated
        )

        if (role == UserRole.ADMIN) {
            observeSafe(
                "staff_profiles",
                db.collection("staff_profiles").snapshots()
                    .map { snap -> snap.documents.mapNotNull { it.toStaffAccountOrNull() } },
                callbacks::onStaffUpdated
            )
        } else if (role == UserRole.STAFF && !email.isNullOrBlank()) {
            observeSafe(
                "staff_profiles",
                db.collection("staff_profiles").whereEqualTo("email", email).snapshots()
                    .map { snap -> snap.documents.mapNotNull { it.toStaffAccountOrNull() } },
                callbacks::onStaffUpdated
            )
        }
    }

    private suspend fun ensureUserDocument(uid: String, displayName: String, email: String, role: UserRole) {
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName.ifBlank { "QEase User" }.take(80),
                    "email" to email.ifBlank { "$uid@qease.gov.in" }.take(120),
                    "lastPortalRole" to role.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.update(
                mapOf(
                    "lastPortalRole" to role.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    private suspend fun ensureAdminIfBootstrapped(uid: String, email: String?, emailVerified: Boolean): Boolean {
        val docRef = db.collection("admins").document(uid)
        if (docRef.get().await().exists()) return true
        if (email == BOOTSTRAP_ADMIN_EMAIL && emailVerified) {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "email" to email,
                    "role" to "ADMIN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            return true
        }
        return false
    }

    private suspend fun claimStaffAccessIfEligible(uid: String, email: String?, emailVerified: Boolean) {
        if (email.isNullOrBlank() || !emailVerified) return
        val accessRef = db.collection("staff_access").document(uid)
        if (accessRef.get().await().exists()) return
        val matches = db.collection("staff_profiles").whereEqualTo("email", email).get().await()
        val profile = matches.documents.firstOrNull { it.getBoolean("active") == true } ?: return
        accessRef.set(
            mapOf(
                "userId" to uid,
                "email" to email,
                "staffProfileId" to profile.id,
                "assignedCentreId" to (profile.getString("assignedCentreId") ?: "centre_central_psk"),
                "assignedCounterId" to (profile.getString("assignedCounterId") ?: "cnt_psk_1"),
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private suspend fun seedCatalogIfEmpty(uid: String, seed: SeedSnapshot) {
        val existingCentres = db.collection("centres").whereEqualTo("visibility", "PUBLIC").limit(1).get().await()
        if (!existingCentres.isEmpty) return

        seed.centres.forEach { db.collection("centres").document(it.id).set(it.toFirestoreCreateMap(uid)).await() }
        seed.services.forEach { db.collection("services").document(it.id).set(it.toFirestoreCreateMap(uid)).await() }
        seed.counters.forEach { db.collection("counters").document(it.id).set(it.toFirestoreCreateMap(uid)).await() }
        seed.staffAccounts.forEach { db.collection("staff_profiles").document(it.id).set(it.toStaffProfileCreateMap(uid)).await() }
        db.collection("system_settings").document("global").set(seed.settings.toFirestoreCreateMap()).await()
    }

    // =====================================================
    // SAFE MUTATION WRAPPERS (RESULT<UNIT>, NEVER THROW)
    // =====================================================

    fun launchWrite(operationType: OperationType, path: String, block: suspend (uid: String) -> Unit) {
        if (auth.currentUser?.uid.isNullOrBlank()) return
        syncScope.launch {
            safeWrite(operationType, path, block)
        }
    }

    suspend fun safeWrite(operationType: OperationType, path: String, block: suspend (uid: String) -> Unit): Result<Unit> {
        return try {
            val uid = requireUserId()
            block(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, operationType, path)
            _lastSyncError.value = e.localizedMessage
            Result.failure(e)
        }
    }

    fun pushTokenCreated(token: QueueToken) = launchWrite(OperationType.CREATE, "tokens/${token.id}") { uid ->
        db.collection("tokens").document(token.id).set(token.toFirestoreCreateMap(uid)).await()
    }

    fun pushTokenCancelled(tokenId: String) = launchWrite(OperationType.UPDATE, "tokens/$tokenId") { _ ->
        db.collection("tokens").document(tokenId).update(
            mapOf(
                "status" to TokenStatus.CANCELLED.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushTokenUpdated(token: QueueToken) = launchWrite(OperationType.UPDATE, "tokens/${token.id}") { _ ->
        val payload = mapOf(
            "status" to token.status.name,
            "counterNumber" to token.counterNumber,
            "counterName" to token.counterName,
            "positionInQueue" to token.positionInQueue,
            "peopleAhead" to token.peopleAhead,
            "estimatedWaitMin" to token.estimatedWaitMin,
            "estimatedWaitMax" to token.estimatedWaitMax,
            "calledAtTimestamp" to token.calledAtTimestamp,
            "servingAtTimestamp" to token.servingAtTimestamp,
            "completedAtTimestamp" to token.completedAtTimestamp,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        db.collection("tokens").document(token.id).update(payload).await()
    }

    fun pushCounterUpdated(counter: Counter) = launchWrite(OperationType.UPDATE, "counters/${counter.id}") { uid ->
        val payload = mapOf(
            "active" to counter.isActive,
            "currentServingToken" to counter.currentServingToken,
            "updatedByUid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        db.collection("counters").document(counter.id).update(payload).await()
    }

    fun pushProposalCreated(proposal: ServiceChangeProposal) = launchWrite(OperationType.CREATE, "service_change_proposals/${proposal.id}") { uid ->
        db.collection("service_change_proposals").document(proposal.id).set(proposal.toFirestoreCreateMap(uid)).await()
    }

    fun pushProposalReviewed(proposal: ServiceChangeProposal) = launchWrite(OperationType.UPDATE, "service_change_proposals/${proposal.id}") { _ ->
        val payload = mapOf(
            "status" to proposal.status.name,
            "reviewedAt" to proposal.reviewedAt,
            "reviewedByAdminName" to proposal.reviewedByAdminName,
            "adminReviewNote" to proposal.adminReviewNote,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        db.collection("service_change_proposals").document(proposal.id).update(payload).await()
    }

    fun pushServiceUpdated(service: ServiceItem) = launchWrite(OperationType.UPDATE, "services/${service.id}") { uid ->
        db.collection("services").document(service.id).update(service.toFirestoreUpdateMap(uid)).await()
    }

    fun pushCentreUpserted(centre: ServiceCentre, isCreate: Boolean) = launchWrite(
        if (isCreate) OperationType.CREATE else OperationType.UPDATE,
        "centres/${centre.id}"
    ) { uid ->
        val ref = db.collection("centres").document(centre.id)
        if (isCreate) {
            ref.set(centre.toFirestoreCreateMap(uid)).await()
        } else {
            val payload = mapOf(
                "name" to centre.name,
                "category" to centre.category.name,
                "address" to centre.address,
                "operatingHours" to centre.operatingHours,
                "phone" to centre.phone,
                "activeCountersCount" to centre.activeCountersCount,
                "totalCountersCount" to centre.totalCountersCount,
                "currentCrowdLevel" to centre.currentCrowdLevel.name,
                "estimatedWaitMin" to centre.estimatedWaitMin,
                "estimatedWaitMax" to centre.estimatedWaitMax,
                "lastCrowdUpdateMinutesAgo" to centre.lastCrowdUpdateMinutesAgo,
                "crowdConfidencePercent" to centre.crowdConfidencePercent,
                "bestTimeToVisit" to centre.bestTimeToVisit,
                "virtualQueueOpen" to centre.isVirtualQueueOpen,
                "availableServiceIds" to centre.availableServiceIds,
                "rating" to centre.rating,
                "totalReviewsCount" to centre.totalReviewsCount,
                "liveNotice" to centre.liveNotice,
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.update(payload).await()
        }
    }

    fun pushCentreRatingUpdated(centreId: String, rating: Double, totalReviewsCount: Int) = launchWrite(
        OperationType.UPDATE,
        "centres/$centreId"
    ) { _ ->
        db.collection("centres").document(centreId).update(
            mapOf(
                "rating" to rating,
                "totalReviewsCount" to totalReviewsCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushCentreCrowdMetricsUpdated(centre: ServiceCentre) = launchWrite(
        OperationType.UPDATE,
        "centres/${centre.id}"
    ) { uid ->
        db.collection("centres").document(centre.id).update(
            mapOf(
                "activeCountersCount" to centre.activeCountersCount,
                "crowdConfidencePercent" to centre.crowdConfidencePercent,
                "lastCrowdUpdateMinutesAgo" to centre.lastCrowdUpdateMinutesAgo,
                "currentCrowdLevel" to centre.currentCrowdLevel.name,
                "estimatedWaitMin" to centre.estimatedWaitMin,
                "estimatedWaitMax" to centre.estimatedWaitMax,
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushStaffProfileUpserted(staff: UserAccount, isCreate: Boolean) = launchWrite(
        if (isCreate) OperationType.CREATE else OperationType.UPDATE,
        "staff_profiles/${staff.id}"
    ) { uid ->
        val ref = db.collection("staff_profiles").document(staff.id)
        if (isCreate) {
            ref.set(staff.toStaffProfileCreateMap(uid)).await()
        } else {
            ref.update(
                mapOf(
                    "name" to staff.name,
                    "email" to staff.email,
                    "phone" to staff.phone,
                    "assignedCentreId" to (staff.assignedCentreId ?: "centre_central_psk"),
                    "assignedCounterId" to (staff.assignedCounterId ?: "cnt_psk_1"),
                    "department" to staff.department,
                    "pin" to staff.pin,
                    "active" to staff.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    fun pushSystemSettings(settings: SystemSettings) = launchWrite(OperationType.UPDATE, "system_settings/global") { _ ->
        val ref = db.collection("system_settings").document("global")
        if (ref.get().await().exists()) {
            ref.update(
                mapOf(
                    "autoNoShowTimeoutMinutes" to settings.autoNoShowTimeoutMinutes,
                    "maxActiveTokensPerCitizen" to settings.maxActiveTokensPerCitizen,
                    "crowdDecayHalfLifeMinutes" to settings.crowdDecayHalfLifeMinutes,
                    "walkInBufferPerHour" to settings.walkInBufferPerHour,
                    "maintenanceMode" to settings.maintenanceMode,
                    "maintenanceBanner" to settings.maintenanceBanner,
                    "updatedAtMillis" to settings.updatedAt,
                    "updatedByAdmin" to settings.updatedByAdmin,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(settings.toFirestoreCreateMap()).await()
        }
    }

    fun pushNotificationCreated(notif: StaffNotification) = launchWrite(OperationType.CREATE, "notifications/${notif.id}") { uid ->
        db.collection("notifications").document(notif.id).set(notif.toFirestoreCreateMap(uid)).await()
    }

    fun pushNotificationRead(notificationId: String) = launchWrite(OperationType.UPDATE, "notifications/$notificationId") { _ ->
        db.collection("notifications").document(notificationId).update(
            mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    fun pushCrowdReportCreated(report: CrowdReport) = launchWrite(OperationType.CREATE, "crowd_reports/${report.id}") { uid ->
        db.collection("crowd_reports").document(report.id).set(report.toFirestoreCreateMap(uid)).await()
    }

    fun pushCrowdReportVerified(reportId: String, confidenceScore: Int) = launchWrite(OperationType.UPDATE, "crowd_reports/$reportId") { _ ->
        db.collection("crowd_reports").document(reportId).update(
            mapOf(
                "staffVerified" to true,
                "confidenceScore" to confidenceScore,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushCrowdReportUpvoted(reportId: String, upvotesCount: Int) = launchWrite(OperationType.UPDATE, "crowd_reports/$reportId") { _ ->
        db.collection("crowd_reports").document(reportId).update(
            mapOf(
                "upvotesCount" to upvotesCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushReviewCreated(review: CentreReview) = launchWrite(OperationType.CREATE, "reviews/${review.id}") { uid ->
        db.collection("reviews").document(review.id).set(review.toFirestoreCreateMap(uid)).await()
    }

    fun pushReviewUpvoted(reviewId: String, helpfulCount: Int) = launchWrite(OperationType.UPDATE, "reviews/$reviewId") { _ ->
        db.collection("reviews").document(reviewId).update(
            mapOf(
                "helpfulCount" to helpfulCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushLiveUpdateCreated(update: LiveQueueStatusUpdate) = launchWrite(OperationType.CREATE, "live_queue_updates/${update.id}") { uid ->
        db.collection("live_queue_updates").document(update.id).set(update.toFirestoreCreateMap(uid)).await()
    }

    fun pushLiveUpdateUpvoted(updateId: String, upvotes: Int) = launchWrite(OperationType.UPDATE, "live_queue_updates/$updateId") { _ ->
        db.collection("live_queue_updates").document(updateId).update(
            mapOf(
                "upvotes" to upvotes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun pushAuditLog(log: AuditLog) = launchWrite(OperationType.CREATE, "audit_logs/${log.id}") { uid ->
        db.collection("audit_logs").document(log.id).set(log.toFirestoreCreateMap(uid)).await()
    }

    companion object {
        private const val TAG = "FirebaseCloudSync"
        private const val BOOTSTRAP_ADMIN_EMAIL = "vikasharmy811@gmail.com"

        fun createIfAvailable(context: Context): FirebaseCloudSync? {
            return runCatching {
                if (!isFirebaseInitialized(context)) return null
                FirebaseCloudSync(context.applicationContext)
            }.getOrNull()
        }
    }
}
