package com.example.data.remote

import com.example.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.filterNonNullValues(): Map<String, Any> =
    filterValues { it != null } as Map<String, Any>

private fun Any?.asInt(default: Int = 0): Int = (this as? Number)?.toInt() ?: default
private fun Any?.asLong(default: Long = 0L): Long = (this as? Number)?.toLong() ?: default
private fun Any?.asDouble(default: Double = 0.0): Double = (this as? Number)?.toDouble() ?: default
private fun Any?.asBool(default: Boolean = false): Boolean = this as? Boolean ?: default

private inline fun <reified T : Enum<T>> safeEnum(raw: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default

fun ServiceItem.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "category" to category.name,
    "title" to title,
    "code" to code,
    "description" to description,
    "officialFee" to officialFee,
    "avgProcessingDurationMin" to avgProcessingDurationMin,
    "eligibility" to eligibility,
    "proceduralSteps" to proceduralSteps,
    "requiredDocuments" to requiredDocuments.map { doc ->
        mapOf(
            "id" to doc.id,
            "name" to doc.name,
            "description" to doc.description,
            "mandatory" to doc.isMandatory,
            "acceptableAlternatives" to doc.acceptableAlternatives,
            "remarks" to doc.remarks
        )
    },
    "available" to isAvailable,
    "cautionNote" to cautionNote,
    "visibility" to "PUBLIC",
    "updatedByUid" to uid,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun ServiceItem.toFirestoreUpdateMap(uid: String): Map<String, Any> = mapOf(
    "category" to category.name,
    "title" to title,
    "code" to code,
    "description" to description,
    "officialFee" to officialFee,
    "avgProcessingDurationMin" to avgProcessingDurationMin,
    "eligibility" to eligibility,
    "proceduralSteps" to proceduralSteps,
    "requiredDocuments" to requiredDocuments.map { doc ->
        mapOf(
            "id" to doc.id,
            "name" to doc.name,
            "description" to doc.description,
            "mandatory" to doc.isMandatory,
            "acceptableAlternatives" to doc.acceptableAlternatives,
            "remarks" to doc.remarks
        )
    },
    "available" to isAvailable,
    "cautionNote" to cautionNote,
    "updatedByUid" to uid,
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toServiceItemOrNull(): ServiceItem? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    val rawDocs = (d["requiredDocuments"] as? List<*>)?.mapNotNull { entry ->
        val m = entry as? Map<String, Any?> ?: return@mapNotNull null
        RequiredDocument(
            id = m["id"] as? String ?: "",
            name = m["name"] as? String ?: "",
            description = m["description"] as? String ?: "",
            isMandatory = (m["mandatory"] as? Boolean) ?: (m["isMandatory"] as? Boolean) ?: true,
            acceptableAlternatives = (m["acceptableAlternatives"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            remarks = m["remarks"] as? String ?: ""
        )
    }.orEmpty()
    return ServiceItem(
        id = id,
        category = safeEnum(d["category"] as? String, ServiceCategory.CIVIL_ID),
        title = d["title"] as? String ?: return null,
        code = d["code"] as? String ?: "",
        description = d["description"] as? String ?: "",
        officialFee = d["officialFee"] as? String ?: "",
        avgProcessingDurationMin = d["avgProcessingDurationMin"].asInt(15),
        requiredDocuments = rawDocs,
        eligibility = d["eligibility"] as? String ?: "",
        proceduralSteps = (d["proceduralSteps"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        isAvailable = d["available"].asBool(true),
        cautionNote = d["cautionNote"] as? String ?: ""
    )
}

fun ServiceCentre.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "name" to name,
    "category" to category.name,
    "address" to address,
    "latitude" to latitude,
    "longitude" to longitude,
    "distanceKm" to distanceKm,
    "operatingHours" to operatingHours,
    "phone" to phone,
    "activeCountersCount" to activeCountersCount,
    "totalCountersCount" to totalCountersCount,
    "currentCrowdLevel" to currentCrowdLevel.name,
    "estimatedWaitMin" to estimatedWaitMin,
    "estimatedWaitMax" to estimatedWaitMax,
    "lastCrowdUpdateMinutesAgo" to lastCrowdUpdateMinutesAgo,
    "crowdConfidencePercent" to crowdConfidencePercent,
    "bestTimeToVisit" to bestTimeToVisit,
    "virtualQueueOpen" to isVirtualQueueOpen,
    "availableServiceIds" to availableServiceIds,
    "rating" to rating,
    "totalReviewsCount" to totalReviewsCount,
    "liveNotice" to liveNotice,
    "visibility" to "PUBLIC",
    "updatedByUid" to uid,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toServiceCentreOrNull(): ServiceCentre? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return ServiceCentre(
        id = id,
        name = d["name"] as? String ?: return null,
        category = safeEnum(d["category"] as? String, ServiceCategory.CIVIL_ID),
        address = d["address"] as? String ?: "",
        latitude = d["latitude"].asDouble(),
        longitude = d["longitude"].asDouble(),
        distanceKm = d["distanceKm"].asDouble(1.0),
        operatingHours = d["operatingHours"] as? String ?: "",
        phone = d["phone"] as? String ?: "",
        activeCountersCount = d["activeCountersCount"].asInt(1),
        totalCountersCount = d["totalCountersCount"].asInt(1),
        currentCrowdLevel = safeEnum(d["currentCrowdLevel"] as? String, CrowdLevel.MODERATE),
        estimatedWaitMin = d["estimatedWaitMin"].asInt(10),
        estimatedWaitMax = d["estimatedWaitMax"].asInt(15),
        lastCrowdUpdateMinutesAgo = d["lastCrowdUpdateMinutesAgo"].asInt(1),
        crowdConfidencePercent = d["crowdConfidencePercent"].asInt(90),
        bestTimeToVisit = d["bestTimeToVisit"] as? String ?: "",
        isVirtualQueueOpen = d["virtualQueueOpen"].asBool(true),
        availableServiceIds = (d["availableServiceIds"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        rating = d["rating"].asDouble(4.7),
        totalReviewsCount = d["totalReviewsCount"].asInt(0),
        liveNotice = d["liveNotice"] as? String
    )
}

fun Counter.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "centreId" to centreId,
    "counterNumber" to counterNumber,
    "name" to name,
    "assignedStaffName" to assignedStaffName,
    "active" to isActive,
    "currentServingToken" to currentServingToken,
    "handledServiceIds" to handledServiceIds,
    "visibility" to "PUBLIC",
    "updatedByUid" to uid,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toCounterOrNull(): Counter? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return Counter(
        id = id,
        centreId = d["centreId"] as? String ?: return null,
        counterNumber = d["counterNumber"].asInt(1),
        name = d["name"] as? String ?: "",
        assignedStaffName = d["assignedStaffName"] as? String ?: "",
        isActive = d["active"].asBool(true),
        currentServingToken = d["currentServingToken"] as? String,
        handledServiceIds = (d["handledServiceIds"] as? List<*>)?.filterIsInstance<String>().orEmpty()
    )
}

fun UserAccount.toStaffProfileCreateMap(adminUid: String): Map<String, Any> = mapOf(
    "name" to name,
    "email" to email,
    "phone" to phone,
    "assignedCentreId" to (assignedCentreId ?: "centre_central_psk"),
    "assignedCounterId" to (assignedCounterId ?: "cnt_psk_1"),
    "department" to department,
    "pin" to pin,
    "active" to isActive,
    "createdByUid" to adminUid,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toStaffAccountOrNull(): UserAccount? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return UserAccount(
        id = id,
        name = d["name"] as? String ?: return null,
        email = d["email"] as? String ?: "",
        phone = d["phone"] as? String ?: "",
        role = UserRole.STAFF,
        assignedCentreId = d["assignedCentreId"] as? String,
        assignedCounterId = d["assignedCounterId"] as? String,
        pin = d["pin"] as? String ?: "",
        department = d["department"] as? String ?: "",
        isLoggedIn = false,
        isActive = d["active"].asBool(true)
    )
}

fun QueueToken.toFirestoreCreateMap(ownerUid: String): Map<String, Any> = mapOf(
    "tokenNumber" to tokenNumber,
    "centreId" to centreId,
    "centreName" to centreName,
    "serviceId" to serviceId,
    "serviceName" to serviceName,
    "userId" to ownerUid,
    "userName" to userName,
    "userPhone" to userPhone,
    "status" to status.name,
    "counterNumber" to counterNumber,
    "counterName" to counterName,
    "positionInQueue" to positionInQueue,
    "peopleAhead" to peopleAhead,
    "estimatedWaitMin" to estimatedWaitMin,
    "estimatedWaitMax" to estimatedWaitMax,
    "joinedAtTimestamp" to joinedAtTimestamp,
    "calledAtTimestamp" to calledAtTimestamp,
    "servingAtTimestamp" to servingAtTimestamp,
    "completedAtTimestamp" to completedAtTimestamp,
    "qrVerificationCode" to qrVerificationCode,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toQueueTokenOrNull(): QueueToken? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return QueueToken(
        id = id,
        tokenNumber = d["tokenNumber"] as? String ?: return null,
        centreId = d["centreId"] as? String ?: "",
        centreName = d["centreName"] as? String ?: "",
        serviceId = d["serviceId"] as? String ?: "",
        serviceName = d["serviceName"] as? String ?: "",
        userId = d["userId"] as? String ?: "",
        userName = d["userName"] as? String ?: "",
        userPhone = d["userPhone"] as? String ?: "",
        status = safeEnum(d["status"] as? String, TokenStatus.WAITING),
        counterNumber = (d["counterNumber"] as? Number)?.toInt(),
        counterName = d["counterName"] as? String,
        positionInQueue = d["positionInQueue"].asInt(1),
        peopleAhead = d["peopleAhead"].asInt(0),
        estimatedWaitMin = d["estimatedWaitMin"].asInt(5),
        estimatedWaitMax = d["estimatedWaitMax"].asInt(10),
        joinedAtTimestamp = d["joinedAtTimestamp"].asLong(System.currentTimeMillis()),
        calledAtTimestamp = (d["calledAtTimestamp"] as? Number)?.toLong(),
        servingAtTimestamp = (d["servingAtTimestamp"] as? Number)?.toLong(),
        completedAtTimestamp = (d["completedAtTimestamp"] as? Number)?.toLong(),
        qrVerificationCode = d["qrVerificationCode"] as? String ?: "QEASE-TKN-$id"
    )
}

fun ServiceChangeProposal.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "proposalType" to proposalType.name,
    "serviceId" to serviceId,
    "serviceTitle" to serviceTitle,
    "centreId" to centreId,
    "submittedByStaffId" to submittedByStaffId,
    "submittedByUid" to uid,
    "submittedByStaffName" to submittedByStaffName,
    "assignedCounterNumber" to assignedCounterNumber,
    "rationale" to rationale,
    "status" to status.name,
    "submittedAt" to submittedAt,
    "reviewedAt" to reviewedAt,
    "reviewedByAdminName" to reviewedByAdminName,
    "adminReviewNote" to adminReviewNote,
    "previousFee" to previousFee,
    "proposedFee" to proposedFee,
    "previousDurationMin" to previousDurationMin,
    "proposedDurationMin" to proposedDurationMin,
    "previousDescription" to previousDescription,
    "proposedDescription" to proposedDescription,
    "previousEligibility" to previousEligibility,
    "proposedEligibility" to proposedEligibility,
    "previousCautionNote" to previousCautionNote,
    "proposedCautionNote" to proposedCautionNote,
    "documentId" to documentId,
    "previousDocName" to previousDocName,
    "proposedDocName" to proposedDocName,
    "previousDocDescription" to previousDocDescription,
    "proposedDocDescription" to proposedDocDescription,
    "previousDocMandatory" to previousDocMandatory,
    "proposedDocMandatory" to proposedDocMandatory,
    "previousDocAlternatives" to previousDocAlternatives,
    "proposedDocAlternatives" to proposedDocAlternatives,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toProposalOrNull(): ServiceChangeProposal? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return ServiceChangeProposal(
        id = id,
        proposalType = safeEnum(d["proposalType"] as? String, ProposalType.SERVICE_INFO),
        serviceId = d["serviceId"] as? String ?: return null,
        serviceTitle = d["serviceTitle"] as? String ?: "",
        centreId = d["centreId"] as? String ?: "",
        submittedByStaffId = d["submittedByStaffId"] as? String ?: "",
        submittedByStaffName = d["submittedByStaffName"] as? String ?: "",
        assignedCounterNumber = d["assignedCounterNumber"].asInt(1),
        rationale = d["rationale"] as? String ?: "",
        status = safeEnum(d["status"] as? String, ProposalStatus.PENDING),
        submittedAt = d["submittedAt"].asLong(System.currentTimeMillis()),
        reviewedAt = (d["reviewedAt"] as? Number)?.toLong(),
        reviewedByAdminName = d["reviewedByAdminName"] as? String,
        adminReviewNote = d["adminReviewNote"] as? String ?: "",
        previousFee = d["previousFee"] as? String ?: "",
        proposedFee = d["proposedFee"] as? String ?: "",
        previousDurationMin = d["previousDurationMin"].asInt(0),
        proposedDurationMin = d["proposedDurationMin"].asInt(0),
        previousDescription = d["previousDescription"] as? String ?: "",
        proposedDescription = d["proposedDescription"] as? String ?: "",
        previousEligibility = d["previousEligibility"] as? String ?: "",
        proposedEligibility = d["proposedEligibility"] as? String ?: "",
        previousCautionNote = d["previousCautionNote"] as? String ?: "",
        proposedCautionNote = d["proposedCautionNote"] as? String ?: "",
        documentId = d["documentId"] as? String ?: "",
        previousDocName = d["previousDocName"] as? String ?: "",
        proposedDocName = d["proposedDocName"] as? String ?: "",
        previousDocDescription = d["previousDocDescription"] as? String ?: "",
        proposedDocDescription = d["proposedDocDescription"] as? String ?: "",
        previousDocMandatory = d["previousDocMandatory"].asBool(true),
        proposedDocMandatory = d["proposedDocMandatory"].asBool(true),
        previousDocAlternatives = (d["previousDocAlternatives"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        proposedDocAlternatives = (d["proposedDocAlternatives"] as? List<*>)?.filterIsInstance<String>().orEmpty()
    )
}

fun StaffNotification.toFirestoreCreateMap(recipientUid: String): Map<String, Any> = mapOf(
    "title" to title,
    "message" to message,
    "targetRole" to targetRole.name,
    "centreId" to centreId,
    "counterId" to counterId,
    "severity" to severity.name,
    "read" to isRead,
    "timestamp" to timestamp,
    "relatedTokenNumber" to relatedTokenNumber,
    "recipientUid" to recipientUid,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toNotificationOrNull(): StaffNotification? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return StaffNotification(
        id = id,
        title = d["title"] as? String ?: return null,
        message = d["message"] as? String ?: "",
        targetRole = safeEnum(d["targetRole"] as? String, UserRole.STAFF),
        centreId = d["centreId"] as? String,
        counterId = d["counterId"] as? String,
        severity = safeEnum(d["severity"] as? String, NotificationSeverity.INFO),
        isRead = d["read"].asBool(false),
        timestamp = d["timestamp"].asLong(System.currentTimeMillis()),
        relatedTokenNumber = d["relatedTokenNumber"] as? String
    )
}

fun SystemSettings.toFirestoreCreateMap(): Map<String, Any> = mapOf(
    "autoNoShowTimeoutMinutes" to autoNoShowTimeoutMinutes,
    "maxActiveTokensPerCitizen" to maxActiveTokensPerCitizen,
    "crowdDecayHalfLifeMinutes" to crowdDecayHalfLifeMinutes,
    "walkInBufferPerHour" to walkInBufferPerHour,
    "maintenanceMode" to maintenanceMode,
    "maintenanceBanner" to maintenanceBanner,
    "updatedAtMillis" to updatedAt,
    "updatedByAdmin" to updatedByAdmin,
    "visibility" to "PUBLIC",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toSystemSettingsOrNull(): SystemSettings? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return SystemSettings(
        id = id,
        autoNoShowTimeoutMinutes = d["autoNoShowTimeoutMinutes"].asInt(15),
        maxActiveTokensPerCitizen = d["maxActiveTokensPerCitizen"].asInt(2),
        crowdDecayHalfLifeMinutes = d["crowdDecayHalfLifeMinutes"].asInt(20),
        walkInBufferPerHour = d["walkInBufferPerHour"].asInt(4),
        maintenanceMode = d["maintenanceMode"].asBool(false),
        maintenanceBanner = d["maintenanceBanner"] as? String ?: "",
        updatedAt = d["updatedAtMillis"].asLong(System.currentTimeMillis()),
        updatedByAdmin = d["updatedByAdmin"] as? String ?: "System Administrator"
    )
}

fun CrowdReport.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "centreId" to centreId,
    "centreName" to centreName,
    "crowdLevel" to crowdLevel.name,
    "approxPeopleCount" to approxPeopleCount,
    "reportedWaitMinutes" to reportedWaitMinutes,
    "notes" to notes,
    "reporterName" to reporterName,
    "timestamp" to timestamp,
    "upvotesCount" to upvotesCount,
    "staffVerified" to isStaffVerified,
    "confidenceScore" to confidenceScore,
    "visibility" to "PUBLIC",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toCrowdReportOrNull(): CrowdReport? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return CrowdReport(
        id = id,
        centreId = d["centreId"] as? String ?: return null,
        centreName = d["centreName"] as? String ?: "",
        crowdLevel = safeEnum(d["crowdLevel"] as? String, CrowdLevel.MODERATE),
        approxPeopleCount = d["approxPeopleCount"].asInt(10),
        reportedWaitMinutes = d["reportedWaitMinutes"].asInt(10),
        notes = d["notes"] as? String ?: "",
        reporterName = d["reporterName"] as? String ?: "",
        timestamp = d["timestamp"].asLong(System.currentTimeMillis()),
        upvotesCount = d["upvotesCount"].asInt(1),
        isStaffVerified = d["staffVerified"].asBool(false),
        confidenceScore = d["confidenceScore"].asInt(90)
    )
}

fun CentreReview.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "centreId" to centreId,
    "centreName" to centreName,
    "userName" to userName,
    "rating" to rating,
    "reviewText" to reviewText,
    "visitTag" to visitTag,
    "queueSpeedRating" to queueSpeedRating,
    "timestamp" to timestamp,
    "helpfulCount" to helpfulCount,
    "verifiedVisitor" to isVerifiedVisitor,
    "visibility" to "PUBLIC",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toCentreReviewOrNull(): CentreReview? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return CentreReview(
        id = id,
        centreId = d["centreId"] as? String ?: return null,
        centreName = d["centreName"] as? String ?: "",
        userId = d["userId"] as? String ?: "",
        userName = d["userName"] as? String ?: "",
        rating = d["rating"].asInt(5),
        reviewText = d["reviewText"] as? String ?: "",
        visitTag = d["visitTag"] as? String ?: "General Visit",
        queueSpeedRating = d["queueSpeedRating"] as? String ?: "Normal",
        timestamp = d["timestamp"].asLong(System.currentTimeMillis()),
        helpfulCount = d["helpfulCount"].asInt(0),
        isVerifiedVisitor = d["verifiedVisitor"].asBool(true)
    )
}

fun LiveQueueStatusUpdate.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "centreId" to centreId,
    "centreName" to centreName,
    "reporterName" to reporterName,
    "updateTag" to updateTag,
    "message" to message,
    "timestamp" to timestamp,
    "currentServingEstimate" to currentServingEstimate,
    "upvotes" to upvotes,
    "visibility" to "PUBLIC",
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toLiveUpdateOrNull(): LiveQueueStatusUpdate? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return LiveQueueStatusUpdate(
        id = id,
        centreId = d["centreId"] as? String ?: return null,
        centreName = d["centreName"] as? String ?: "",
        reporterName = d["reporterName"] as? String ?: "",
        updateTag = d["updateTag"] as? String ?: "ON_TRACK",
        message = d["message"] as? String ?: "",
        timestamp = d["timestamp"].asLong(System.currentTimeMillis()),
        currentServingEstimate = d["currentServingEstimate"] as? String ?: "",
        upvotes = d["upvotes"].asInt(1)
    )
}

fun AuditLog.toFirestoreCreateMap(uid: String): Map<String, Any> = mapOf(
    "timestamp" to timestamp,
    "action" to action,
    "actorName" to actorName,
    "actorRole" to actorRole.name,
    "actorUid" to uid,
    "details" to details,
    "centreId" to centreId,
    "createdAt" to FieldValue.serverTimestamp()
).filterNonNullValues()

fun DocumentSnapshot.toAuditLogOrNull(): AuditLog? {
    val d = getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
    return AuditLog(
        id = id,
        timestamp = d["timestamp"].asLong(System.currentTimeMillis()),
        action = d["action"] as? String ?: return null,
        actorName = d["actorName"] as? String ?: "",
        actorRole = safeEnum(d["actorRole"] as? String, UserRole.CUSTOMER),
        details = d["details"] as? String ?: "",
        centreId = d["centreId"] as? String
    )
}
