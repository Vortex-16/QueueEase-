# QEase Architecture & Engineering Decisions Document (ADR)

**Project Name:** QEase  
**Subtitle:** Community-Powered Queue & Service Intelligence Platform  
**Target Semester:** Semester-Long Software Development & Engineering Capstone  
**Version:** 1.0.0-PROD  
**Actors:** Customer (Citizen), Service Provider (Staff / Counter Operator), System Administrator  

---

## 1. Executive Summary & Core Product Vision
QEase is a smart, community-powered queue and service intelligence platform designed to eliminate the uncertainty and hours wasted waiting at high-demand public and private service centres (government agencies, hospitals, banks, colleges, municipal offices, diagnostic labs, transport/RTO centres).

Instead of treating queues as a simple ticket or booking list, QEase provides an end-to-end intelligence pipeline answering:
1. **What service** the user needs and **what documents/eligibility/fees** are required before leaving home.
2. **Where** to get it (comparing nearby centres based on real-time crowd level, active counters, and distance).
3. **When** it is best to visit (historical peak-hour analytics and recommendation engine).
4. **Where the user is** in the virtual queue with live position tracking and atomic turn updates.
5. **Community-validated crowd intelligence** with decay scoring, freshness indicators, and staff verification.
6. **Grounded AI/RAG Service Assistant** backed strictly by verified document guidelines to prevent hallucination.

---

## 2. System Architecture & Tech Stack Synthesis

```
                                  +------------------------------------+
                                  |           QEase Platform           |
                                  +------------------------------------+
                                             /               \
                       +--------------------+                 +--------------------+
                       |    React Web App   |                 | Flutter/Android App|
                       |  (Staff/Admin/Web) |                 | (Citizen/Provider) |
                       +--------------------+                 +--------------------+
                                             \               /
                                  +------------------------------------+
                                  |     Serverpod / Dart Backend       |
                                  |     - Auth, RBAC, Sessions         |
                                  |     - Queue State Engine           |
                                  |     - Crowd Confidence Engine      |
                                  |     - Wait-Time Estimator          |
                                  |     - AI / RAG Orchestrator        |
                                  +------------------------------------+
                                             /        |       \
               +-----------------------------+        |        +-----------------------------+
               |                                      |                                      |
  +--------------------------+          +--------------------------+           +--------------------------+
  |    PostgreSQL Database   |          |  Deterministic Queue SM  |           |      Vector / RAG DB     |
  |  - Centres, Services     |          |  - Atomic transitions    |           |  - Service Guidelines    |
  |  - Tokens, Counters      |          |  - Race-condition safe   |           |  - Document Rules, FAQs  |
  |  - Crowd Reports, Audit  |          |  - Append-only event log |           |  - Gemini 3.5 Grounding  |
  +--------------------------+          +--------------------------+           +--------------------------+
```

### 2.1 Multi-Tier Platform Strategy
- **Mobile Client:** Premier Android native / Flutter architecture built with Jetpack Compose Material Design 3, M3 dynamic colors, edge-to-edge rendering, offline Room DB caching, and reactive StateFlows.
- **Backend Protocol:** REST & WebSocket event bus powered by Dart Serverpod backend, with strict server-side RBAC scoping (Centre Staff can only manage their assigned centre; Customers cannot escalate privileges).
- **Primary Data Layer:** PostgreSQL with normalized schema, tenant/centre foreign keys, check constraints, and append-only queue event audit logs.
- **Vector & Intelligence Layer:** Astra DB / pgvector semantic search over verified government & institutional service documents, powering the zero-hallucination Gemini assistant.

---

## 3. Actor Models & Role-Based Access Control (RBAC)

### 3.1 Actor Matrix & Scoping

| Feature / Resource | Customer / Citizen | Service Provider (Staff) | System Administrator |
|---|---|---|---|
| Service Discovery & Requirements Search | Full Read | Full Read | Full Read |
| View Centre Wait Time & Crowd Freshness | Full Read | Full Read | Full Read |
| Join Virtual Queue & Digital Token | Create, Read Own, Cancel Own | Read Centre Tokens | Read All |
| Call Next Customer (Atomic) | Denied | Scoped to Assigned Counter | Supervised Read |
| Mark Serving / Complete / No-Show | Denied | Scoped to Assigned Counter | Override Access |
| Open / Close Service Counters | Denied | Scoped to Assigned Centre | Supervised Read |
| Submit Community Crowd Report | Create (Rate-Limited) | Verify / Override | Moderate / Delete |
| Centre Management & Hours | Read Only | Update Notices (Assigned) | Full CRUD |
| Service Catalog & Document Rules | Read Only | Propose Edits | Full CRUD |
| AI Knowledge Base & RAG Ingestion | Query Only | Read Only | Manage & Ingest |
| Platform Analytics & Audit Logs | Aggregate Read | Centre Operational Read | Full System Analytics |

---

## 4. Deterministic Queue Engine & State Machine

### 4.1 Token State Transitions

```
                 [ User Joins ]
                       │
                       ▼
                 ┌───────────┐
                 │  WAITING  │◄───────────────────┐
                 └─────┬─────┘                    │
                       │ [ Staff: Call Next ]     │ [ Re-queue / Return ]
                       ▼                          │
                 ┌───────────┐                    │
                 │  CALLED   ├────────────────────┘
                 └─────┬─────┴────────────┐
                       │                  │
   [ Staff: Start ]    │                  │ [ Staff: Customer absent ]
                       ▼                  ▼
                 ┌───────────┐      ┌───────────┐
                 │  SERVING  │      │  NO_SHOW  │
                 └─────┬─────┘      └───────────┘
                       │
   [ Staff: Finish ]   │
                       ▼
                 ┌───────────┐
                 │ COMPLETED │
                 └───────────┘

* User can issue [ CANCELLED ] only while in WAITING or CALLED state.
* Completed tokens can NEVER transition back or alter history.
```

### 4.2 Race-Condition Safety ("Call Next" Operation)
- The "Call Next" transaction utilizes PostgreSQL row-level locks (`SELECT ... FOR UPDATE SKIP LOCKED` / atomic state transition) to guarantee that when multiple staff members simultaneously click "Call Next", no two counters will ever be assigned the same token.
- Every state transition produces an immutable record in `queue_events` with `token_id`, `from_state`, `to_state`, `actor_id`, `counter_id`, and `timestamp_utc`.

---

## 5. Waiting Time & Crowd Intelligence Algorithms

### 5.1 Deterministic Waiting Time Estimation Formula
Unlike black-box models, QEase employs a transparent, reproducible multi-factor formula:

$$T_{\text{wait}} = \left( \frac{N_{\text{ahead}} \times \mu_{\text{service}}}{C_{\text{active}}} \right) \times \Phi_{\text{tod}} \times \Psi_{\text{complexity}}$$

Where:
- $N_{\text{ahead}}$ = Number of active tokens ahead in the same service category.
- $\mu_{\text{service}}$ = Rolling average service duration (default: 6–8 minutes).
- $C_{\text{active}}$ = Currently active and non-paused counters for that service ($\ge 1$).
- $\Phi_{\text{tod}}$ = Time-of-day peak congestion multiplier ($1.0$ standard, $1.25$ lunch rush, $1.4$ morning rush).
- $\Psi_{\text{complexity}}$ = Service-specific document verification coefficient.

Estimated wait times are always communicated to citizens as realistic ranges (e.g., *"18–25 min"*) rather than exact minutes to set clear expectations.

### 5.2 Community Crowd Confidence & Freshness Scoring
Community reports are aggregated with exponential time-decay:

$$W(t) = e^{-\lambda \Delta t}, \quad \lambda = \frac{\ln(2)}{t_{\text{half-life}}} \quad (t_{\text{half-life}} = 30\text{ min})$$

- $\Delta t \le 15\text{ min}$: Confidence = **High (95%)**, label: *"Updated 4 min ago"*.
- $15 < \Delta t \le 45\text{ min}$: Confidence = **Medium (75%)**, label: *"Updated 28 min ago"*.
- $45 < \Delta t \le 120\text{ min}$: Confidence = **Low (40%)**, label: *"May be outdated"*.
- $\Delta t > 120\text{ min}$: Report flagged stale; fallback to historical time-of-day projection.
- Official staff verification instantly boosts confidence to 100% and resets freshness.

---

## 6. Grounded AI / RAG Architecture & Safety Rules
1. **Grounding First:** The AI Assistant uses verified service dossiers (required proofs of identity, address, fees, forms, eligibility, counter steps) as its sole knowledge source.
2. **Defensive Prompting:** User inputs are treated strictly as data queries. Prompt injection attempts cannot override official document requirements.
3. **No Hallucination Policy:** If official requirements for an obscure or unlisted procedure are absent from the knowledge base, the assistant explicitly states: *"Official verification unavailable; please check directly with the centre counter."*

---

## 7. Resolution of Documentation Conflicts
- **Conflict:** `QueueEase` vs `QEase` naming across historical documents.
  - **Resolution:** Canonical product name is **QEase**. All user-facing strings, code namespaces, app packages, and documentation use QEase.
- **Conflict:** Standalone appointment booking vs live queue intelligence.
  - **Resolution:** QEase prioritizes real-time walk-in queue intelligence, crowd tracking, and virtual queuing while providing structured appointment slots as an integrated option.
