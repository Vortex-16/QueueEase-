package com.example.domain.engine

import com.example.data.model.KnowledgeDocument
import com.example.data.model.ServiceCategory

object RagKnowledgeEngine {

    val knowledgeBase: List<KnowledgeDocument> = listOf(
        KnowledgeDocument(
            id = "kb_passport_renew",
            serviceId = "srv_passport_renew",
            title = "Passport Renewal & Re-Issue Guidelines",
            category = ServiceCategory.CIVIL_ID,
            content = """
                Official Passport Renewal Guidelines:
                1. Required Documents: Original old passport, Self-attested photocopy of first 2 and last 2 pages, Proof of current residence (Aadhaar / Utility Bill / Bank Statement), Annexure E (for change in appearance or signature).
                2. Official Government Fee: Normal renewal (36 pages) = $1,500; Tatkaal scheme = $3,500.
                3. Eligibility: Existing passport holders whose validity has expired or is expiring within 1 year.
                4. Steps at Counter: Counter A (Token Verification & Biometric Fingerprint/Photo), Counter B (Document Verification), Counter C (Granting Officer Approval).
                5. Best Visiting Time: 09:30 AM – 11:00 AM or 03:00 PM – 04:30 PM.
                6. Important Caution: Incomplete address proof is the top reason for file rejection. Ensure utility bills are within 3 months of issue date.
            """.trimIndent(),
            officialAuthority = "Regional Passport Office / Consular Services",
            verifiedDate = "2026-08-01",
            keywords = listOf("passport", "renewal", "re-issue", "tatkaal", "biometric", "address proof")
        ),
        KnowledgeDocument(
            id = "kb_dl_renew",
            serviceId = "srv_dl_renew",
            title = "Driver's License Renewal & Biometric Endorsement",
            category = ServiceCategory.TRANSPORT_RTO,
            content = """
                Official RTO Driving License Renewal Procedure:
                1. Required Documents: Original Driving License, Form 9 Application, Medical Fitness Certificate (Form 1A) for applicants over 40 years, Proof of age and permanent address, 2 passport size photographs.
                2. Official Fee: Standard Renewal = $200; Late renewal penalty applies after grace period of 30 days.
                3. Counter Procedure: Counter 1 (Document Submission & Fee receipt check), Counter 2 (Biometric capture & Digital Signature), Counter 3 (Scrutiny & Endorsement).
                4. Average Waiting Time: 25–40 minutes without virtual token; 10–15 minutes with QEase token.
            """.trimIndent(),
            officialAuthority = "State Transport Authority / RTO",
            verifiedDate = "2026-08-05",
            keywords = listOf("driving license", "dl", "rto", "transport", "renewal", "medical certificate", "form 1a")
        ),
        KnowledgeDocument(
            id = "kb_aadhaar_update",
            serviceId = "srv_aadhaar_update",
            title = "National ID / Aadhaar Biometric & Address Update",
            category = ServiceCategory.CIVIL_ID,
            content = """
                Aadhaar Seva Kendra / Citizen Identity Centre Guidelines:
                1. Required Documents:
                   - For Address Update: Valid proof of address (Voter ID, Passport, Registered Rent Agreement, Bank Passbook with photo).
                   - For Biometric Update (Fingerprint/Iris/Photo): Existing Aadhaar number, No documentary proof required for pure biometric re-scan.
                   - For Date of Birth (DoB) update: Birth Certificate or SSC Marksheet.
                2. Fee: Biometric update = $100; Demographic update = $50; Mandatory biometric update for minors (ages 5 and 15) is FREE.
                3. Operational Advice: High morning rush between 10:00 AM - 1:00 PM. Book virtual token via QEase to receive turn reminder.
            """.trimIndent(),
            officialAuthority = "UIDAI / National Identity Authority",
            verifiedDate = "2026-08-10",
            keywords = listOf("aadhaar", "national id", "biometric", "address update", "fingerprint", "iris", "dob update")
        ),
        KnowledgeDocument(
            id = "kb_hospital_opd",
            serviceId = "srv_hospital_opd",
            title = "Apex General Hospital OPD Consultation & Triage Token",
            category = ServiceCategory.HEALTHCARE,
            content = """
                General Hospital Outpatient Department (OPD) Process:
                1. Required Documents: National Health ID / Patient Registration Card, Previous Medical Records / Prescription slips, Valid Photo Identity Card.
                2. Registration Fee: General OPD = $20; Specialty Clinic = $50.
                3. Triage & Token: Counter 1 issues digital queue token; Triage nurses assess vitals (BP, pulse, temperature) before doctor consultation at Counter 4-8.
                4. Best Visiting Window: 11:30 AM – 01:30 PM has 35% lower crowd compared to early 08:00 AM rush.
            """.trimIndent(),
            officialAuthority = "Department of Public Health & Hospitals",
            verifiedDate = "2026-08-12",
            keywords = listOf("hospital", "opd", "doctor", "health", "consultation", "clinic", "triage", "prescription")
        ),
        KnowledgeDocument(
            id = "kb_property_tax",
            serviceId = "srv_property_tax",
            title = "Municipal Corporation Property Tax Assessment & Clearance",
            category = ServiceCategory.MUNICIPAL_CIVIC,
            content = """
                Municipal Corporation Property Tax Service:
                1. Required Documents: Property ID / Assessment Number, Previous Year Tax Receipt, Title Deed / Sale Deed copy, Khata / Municipal extract.
                2. Modes of Payment: Cash, Demand Draft, Credit/Debit card, UPI at municipal billing counters.
                3. Counter Steps: Counter 2 (Assessment verification & rebate calculation), Counter 3 (Payment Collection & Stamped NOC Generation).
            """.trimIndent(),
            officialAuthority = "Municipal Corporation Revenue Department",
            verifiedDate = "2026-07-28",
            keywords = listOf("property tax", "tax", "municipal", "civic", "khata", "noc", "assessment")
        )
    )

    fun searchGroundedKnowledge(query: String): List<KnowledgeDocument> {
        val cleanTokens = query.lowercase().split(" ", ",", "?", ".", "!", "-", "/")
            .map { it.trim() }
            .filter { it.length > 2 }

        return knowledgeBase.filter { doc ->
            cleanTokens.any { token ->
                doc.title.lowercase().contains(token) ||
                doc.keywords.any { it.contains(token) } ||
                doc.content.lowercase().contains(token)
            }
        }
    }

    fun generateGroundedAnswer(query: String, retrievedDocs: List<KnowledgeDocument>): String {
        if (retrievedDocs.isEmpty()) {
            return "Verified information for your exact request was not found in the official QEase service directory.\n\nPlease check directly with the respective centre desk or browse by service category."
        }

        val primary = retrievedDocs.first()
        val lines = primary.content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .drop(1) // Drop intro title line since we show the official header below

        val formattedBody = lines.joinToString("\n") { line ->
            val cleaned = line.replace(Regex("^\\d+\\.\\s*"), "")
            when {
                cleaned.startsWith("-") -> cleaned
                cleaned.contains(":") -> cleaned
                else -> "- $cleaned"
            }
        }

        return buildString {
            appendLine("## ${primary.title}")
            appendLine("Authority: ${primary.officialAuthority}")
            appendLine()
            appendLine(formattedBody)
            appendLine()
            append("QEase Tip: Join the live virtual queue on QEase to track active counters and receive turn alerts before arriving.")
        }.trim()
    }

    fun generateStaffCopilotAnswer(
        query: String,
        retrievedDocs: List<KnowledgeDocument>,
        centreName: String,
        counterNumber: Int,
        waitingCount: Int,
        pendingProposalsCount: Int,
        unverifiedReportsCount: Int,
        primaryServiceTitle: String,
        primaryServiceFee: String,
        primaryServiceDuration: Int
    ): String {
        val q = query.lowercase()
        return buildString {
            when {
                q.contains("doc") || q.contains("checklist") || q.contains("proof") || q.contains("alternative") -> {
                    appendLine("## Document Rule & Proposal Draft")
                    appendLine("Desk Scope: $centreName • Counter $counterNumber")
                    appendLine("Target Service: $primaryServiceTitle")
                    appendLine()
                    appendLine("Findings:")
                    appendLine("- Field intake shows address proof and medical Form 1-A verification frequently stall counter throughput.")
                    appendLine("- Document edits submitted from Staff Proposals are held safely for Admin approval before publishing to citizens.")
                    appendLine()
                    append("Recommended Action: Use the button below to prefill a Required Document proposal for $primaryServiceTitle with clear alternative proof guidance.")
                }
                q.contains("crowd") || q.contains("verify") || q.contains("report") || q.contains("alert") -> {
                    appendLine("## Live Desk Crowd & Alert Summary")
                    appendLine("Desk Scope: $centreName • Counter $counterNumber")
                    appendLine()
                    appendLine("Live Telemetry:")
                    appendLine("- Citizens Waiting in Queue: $waitingCount")
                    appendLine("- Unverified Field Crowd Reports: $unverifiedReportsCount")
                    appendLine("- Your Pending Proposals: $pendingProposalsCount")
                    appendLine()
                    append("Recommended Action: Verifying field crowd reports locks centre confidence to 100% and calibrates citizen wait estimates immediately.")
                }
                q.contains("propos") || q.contains("fee") || q.contains("duration") || q.contains("caution") || q.contains("delay") || q.contains("update") -> {
                    val suggestedDuration = (primaryServiceDuration - 2).coerceAtLeast(5)
                    appendLine("## Service Info Proposal Draft")
                    appendLine("Target Service: $primaryServiceTitle")
                    appendLine("Current Official Spec: $primaryServiceFee • $primaryServiceDuration mins")
                    appendLine()
                    appendLine("Suggested Update:")
                    appendLine("- Proposed Duration: $suggestedDuration mins (calibrated to current counter speed)")
                    appendLine("- Counter Caution: Carry original ID & self-attested copies to prevent hold at Counter $counterNumber.")
                    appendLine("- Waiting Queue Impact: $waitingCount citizens currently waiting at $centreName.")
                    appendLine()
                    append("Recommended Action: Tap Prefill Service Info Proposal below to review and submit directly to Admin Approvals.")
                }
                retrievedDocs.isNotEmpty() -> {
                    val doc = retrievedDocs.first()
                    appendLine("## Counter SOP Reference: ${doc.title}")
                    appendLine("Desk Scope: $centreName • Counter $counterNumber")
                    appendLine("Authority: ${doc.officialAuthority}")
                    appendLine()
                    doc.content.lines().map { it.trim() }.filter { it.isNotBlank() }.drop(1).forEach { line ->
                        appendLine("- ${line.replace(Regex("^\\d+\\.\\s*"), "")}")
                    }
                    appendLine()
                    append("Queue Context: $waitingCount waiting tokens at $centreName. Propose any checklist clarifications via Proposals.")
                }
                else -> {
                    appendLine("## Counter Desk AI Copilot")
                    appendLine("Assigned Desk: $centreName • Counter $counterNumber")
                    appendLine()
                    appendLine("Current Snapshot:")
                    appendLine("- Active Waiting Queue: $waitingCount citizens")
                    appendLine("- Pending Proposals Awaiting Admin: $pendingProposalsCount")
                    appendLine("- Unverified Crowd Reports: $unverifiedReportsCount")
                    appendLine()
                    append("Ask me to draft a Service Info or Required Document proposal, summarize queue load, or check counter SOP documents.")
                }
            }
        }.trim()
    }

    fun generateAdminAuditAnswer(
        query: String,
        pendingProposalsSummary: List<String>,
        bottleneckSummary: List<String>,
        statsWaiting: Int,
        activeCounters: Int,
        totalCounters: Int,
        maintenanceMode: Boolean,
        recentAuditSummary: List<String>
    ): String {
        val q = query.lowercase()
        return buildString {
            when {
                q.contains("propos") || q.contains("approv") || q.contains("review") || q.contains("risk") -> {
                    appendLine("## Pending Proposal Governance Audit")
                    appendLine("Queue Governance Scope: ${pendingProposalsSummary.size} pending proposals awaiting sign-off")
                    appendLine()
                    if (pendingProposalsSummary.isEmpty()) {
                        appendLine("Status: All submitted Staff edits have been reviewed and resolved.")
                    } else {
                        appendLine("Items Requiring Review:")
                        pendingProposalsSummary.forEach { appendLine("- $it") }
                    }
                    appendLine()
                    append("Audit Guidance: Verify fee changes and mandatory document removals against official circulars before publishing live to citizens.")
                }
                q.contains("bottleneck") || q.contains("wait") || q.contains("load") || q.contains("centre") || q.contains("center") -> {
                    appendLine("## Cross-Centre Bottleneck Analysis")
                    appendLine("Network Snapshot: $statsWaiting citizens waiting • $activeCounters/$totalCounters counters active")
                    appendLine()
                    appendLine("Load Breakdown:")
                    bottleneckSummary.forEach { appendLine("- $it") }
                    appendLine()
                    append("Recommended Action: Open idle counters or adjust citizen walk-in buffers on high-wait facilities via Ops or Monitor.")
                }
                q.contains("maint") || q.contains("policy") || q.contains("setting") || q.contains("staff") -> {
                    appendLine("## Policy & Operations Audit")
                    appendLine("Global Maintenance Mode: ${if (maintenanceMode) "ACTIVE (Bookings Paused)" else "OFF (Normal Intake)"}")
                    appendLine("Active Desks: $activeCounters / $totalCounters counters online")
                    appendLine()
                    if (recentAuditSummary.isNotEmpty()) {
                        appendLine("Latest Audit Trail:")
                        recentAuditSummary.take(4).forEach { appendLine("- $it") }
                    }
                    appendLine()
                    append("Recommended Action: Review grace timeout, token caps, or officer counter locks inside the Ops console.")
                }
                else -> {
                    appendLine("## Executive Governance & Audit Summary")
                    appendLine("System State: $statsWaiting waiting citizens • $activeCounters/$totalCounters counters • ${pendingProposalsSummary.size} pending approvals")
                    appendLine("Maintenance Mode: ${if (maintenanceMode) "ACTIVE" else "INACTIVE"}")
                    appendLine()
                    if (bottleneckSummary.isNotEmpty()) {
                        appendLine("Centre Load Highlights:")
                        bottleneckSummary.take(3).forEach { appendLine("- $it") }
                    }
                    if (recentAuditSummary.isNotEmpty()) {
                        appendLine()
                        appendLine("Recent Audit Events:")
                        recentAuditSummary.take(3).forEach { appendLine("- $it") }
                    }
                }
            }
        }.trim()
    }
}
