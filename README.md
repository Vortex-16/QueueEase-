# QEase — Service & Queue Intelligence

> Native Android application built with **Kotlin + Jetpack Compose (Material Design 3)** for real-time civic virtual queuing, live turn tracking, grounded document readiness, community crowd intelligence, Firebase Firestore cloud synchronization, interactive home screen widgets, and role-pure Staff & Admin operations.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Getting Started](#getting-started)
4. [Firebase Cloud Sync & Environment Secrets](#firebase-cloud-sync--environment-secrets)
5. [Authentication & RBAC Portals](#authentication--rbac-portals)
6. [Role-Pure Screens & Navigation](#role-pure-screens--navigation)
7. [Role-Aware AI Assistant & Smart Actions](#role-aware-ai-assistant--smart-actions)
8. [Staff Proposal & Admin Approval Workflow](#staff-proposal--admin-approval-workflow)
9. [Interactive Home Screen Widgets](#interactive-home-screen-widgets)
10. [Core Intelligence Engines](#core-intelligence-engines)
11. [Database & Persistence (Firestore + Room)](#database--persistence-firestore--room)
12. [Adaptive & Responsive Design](#adaptive--responsive-design)
13. [Testing & Verification](#testing--verification)
14. [Common Issues](#common-issues)
15. [Contributing](#contributing)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI Framework | Jetpack Compose + Material Design 3 (`Professional Polish` theme) |
| Architecture | MVVM (`QEaseViewModel` + `StateFlow`), Service Engine (`QEaseApiService`) & Domain Engines |
| Cloud Backend & Auth | Firebase Firestore + Firebase Auth + Jetpack `CredentialManager` Google Sign-In (BOM 34.15.0) |
| Local Offline Cache | AndroidX Room 2.7.0 + KSP (`QEaseDatabase`) |
| Networking & AI | Retrofit 2.12 + OkHttp + Moshi + Role-Aware Gemini Grounded RAG (`RagKnowledgeEngine`) |
| Home Screen Widgets | Android AppWidgets (`StaffDeskWidgetProvider`, `AdminMonitorWidgetProvider`, `WidgetStateBridge`) |
| Async & Reactivity | Kotlin Coroutines + `StateFlow` / `collectAsState` |
| Secrets Management | Secrets Gradle Plugin (`.env` / AI Studio Secrets → `BuildConfig`) |
| Build System | Gradle Kotlin DSL (AGP 9.1.1, `compileSdk 36`, `minSdk 24`, `targetSdk 36`) |
| Testing | JUnit 4 + Robolectric 4.16.1 + Compose UI Test + Roborazzi 1.59.0 |

---

## Project Structure

```
qease/
├── .env.example                                ← Template for API keys (never commit .env)
├── build.gradle.kts                            ← Root Gradle configuration
├── settings.gradle.kts                         ← Project settings (rootProject.name = "QEase")
├── firebase.json                               ← Firebase configuration
├── firebase-blueprint.json                     ← Firestore schema & security blueprint
├── firestore.rules                             ← Zero-trust Firestore security rules
├── firestore.test.js                           ← Firestore rules unit test suite
├── gradle/
│   └── libs.versions.toml                      ← Version catalog for all dependencies
├── docs/
│   └── ARCHITECTURE_DECISIONS.md               ← System architecture, formulas & RBAC ADR
└── app/
    ├── build.gradle.kts                        ← App Gradle config, Secrets & Google Services plugins
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml             ← Permissions, deep links & AppWidget receivers
        │   ├── res/
        │   │   ├── drawable/
        │   │   │   ├── ic_launcher_background.xml
        │   │   │   ├── ic_launcher_foreground.xml
        │   │   │   └── ic_qease_minimal_logo.jpg
        │   │   ├── layout/
        │   │   │   ├── widget_staff_desk.xml   ← Staff Counter Desk home screen widget layout
        │   │   │   └── widget_admin_monitor.xml ← Admin Monitor home screen widget layout
        │   │   ├── xml/
        │   │   │   ├── widget_staff_desk_info.xml
        │   │   │   └── widget_admin_monitor_info.xml
        │   │   └── values/
        │   └── java/com/example/
        │       ├── MainActivity.kt             ← Edge-to-edge entry, widget deep links & role-pure nav shell
        │       ├── data/
        │       │   ├── api/
        │       │   │   └── QEaseApiService.kt  ← LocalQEaseApiEngine, proposals, audit logs & stats
        │       │   ├── local/
        │       │   │   ├── Entities.kt         ← Room entities for offline persistence
        │       │   │   ├── QEaseDao.kt         ← Room DAO queries & reactive Flow streams
        │       │   │   └── QEaseDatabase.kt    ← Singleton Room database instance
        │       │   ├── model/
        │       │   │   └── Models.kt           ← Domain models, roles, proposals, notifications & AI actions
        │       │   ├── remote/
        │       │   │   ├── FirebaseCloudSync.kt ← Firestore real-time listeners & cloud writes
        │       │   │   ├── FirestoreErrorHandler.kt ← Structured Firestore error mapping
        │       │   │   ├── FirestoreMappers.kt ← Typed Firestore document serializers/deserializers
        │       │   │   ├── GeminiApiService.kt ← Role-aware Retrofit + Moshi Gemini API client
        │       │   │   └── GoogleAuthManager.kt ← Credential Manager Google Sign-In integration
        │       │   └── repository/
        │       │       └── QEaseRepository.kt  ← Single source of truth, RBAC auth, AI routing & sync
        │       ├── domain/engine/
        │       │   ├── CrowdConfidenceEngine.kt ← Exponential time-decay crowd freshness scoring
        │       │   ├── QueueStateMachine.kt    ← Deterministic FIFO token state transitions
        │       │   ├── RagKnowledgeEngine.kt   ← Citizen RAG + Staff Copilot & Admin Audit generators
        │       │   └── WaitTimeEstimator.kt    ← Multi-factor wait-time range estimator
        │       ├── widget/
        │       │   ├── StaffDeskWidgetProvider.kt ← Interactive Staff Desk widget (Call Next / Open Alerts)
        │       │   ├── AdminMonitorWidgetProvider.kt ← Admin Monitor widget (Monitor / Pending Approvals)
        │       │   └── WidgetStateBridge.kt    ← SharedPreferences snapshot bridge & deep-link intents
        │       └── ui/
        │           ├── components/
        │           │   ├── AuthRbacDialog.kt   ← Role-locked Citizen OTP & Officer PIN modals
        │           │   ├── QEaseComponents.kt  ← Shared cards, badges, indicators & headers
        │           │   └── ResponsiveLayout.kt ← Window width classes, 125%+ scale metrics, DialogShell
        │           ├── screens/
        │           │   ├── WelcomeScreen.kt    ← Google Cloud Sync + portal selector landing screen
        │           │   ├── CustomerHomeScreen.kt ← Citizen dashboard, active token & shortcuts
        │           │   ├── CentresScreen.kt    ← Facility explorer, comparison & crowd reporting
        │           │   ├── ServicesScreen.kt   ← Service catalog & interactive document checklists
        │           │   ├── MyQueueScreen.kt    ← Virtual queue ticket, counter status & turn alerts
        │           │   ├── WhereIsMyTurnScreen.kt ← Live counter-by-counter station queue radar
        │           │   ├── ReviewsFeedbackScreen.kt ← Community reviews, speed ratings & helpful votes
        │           │   ├── AiAssistantScreen.kt ← Role-aware AI Help / Copilot / Governance Auditor
        │           │   ├── StaffDeskScreen.kt  ← StaffDeskScreen, StaffProposalsScreen & StaffAlertsScreen
        │           │   ├── StaffCatalogProposalSections.kt ← Service/Doc proposal forms & prefill consumer
        │           │   ├── AdminPanelScreen.kt ← AdminMonitorScreen, Approvals, Ops & Analytics screens
        │           │   └── AdminOperationsSections.kt ← Staff roster, Centres/Services & System Settings
        │           ├── theme/
        │           │   ├── Color.kt            ← Professional Polish forest/sage color tokens
        │           │   ├── Theme.kt            ← Material 3 theme & system bar icon contrast
        │           │   └── Type.kt             ← Typography scale
        │           └── viewmodel/
        │               └── QEaseViewModel.kt   ← UI state, strict RBAC tab guards & AI workflow execution
        └── test/java/com/example/
            ├── ExampleUnitTest.kt
            ├── ExampleRobolectricTest.kt
            ├── GreetingScreenshotTest.kt       ← Roborazzi visual snapshot test
            ├── ResponsiveUiRobolectricTest.kt  ← RBAC login gate, logout & 125% scale layout tests
            └── StaffAdminApiAndWidgetTest.kt   ← Proposals, widgets, role-pure nav & AI workflow tests
```

---

## Getting Started

### Prerequisites

- **JDK 17+** (Java 11 compile target)
- **Android SDK** (`compileSdk 36`, `ANDROID_SDK_ROOT` configured)
- **Gradle CLI** (`gradle` installed on PATH) or **Android Studio** (Ladybug / Meerkat+)

### Clone, Configure & Build

```bash
# Clone the repository
git clone https://github.com/Vortex-16/QEase.git
cd QEase

# Copy the environment template (optional if using local RAG fallback)
cp .env.example .env

# Build debug APK
gradle assembleDebug

# Run all unit & Robolectric UI tests
gradle :app:testDebugUnitTest
```

> **Note:** Use `gradle` directly or Android Studio to build. Never store API keys in `local.properties`.

---

## Firebase Cloud Sync & Environment Secrets

### Firebase Firestore & Google Sign-In

QEase syncs live queues, centres, counters, staff proposals, notifications, crowd reports, and system settings in real time with **Firebase Firestore** (provisioned with `firestore.rules` and `firebase-blueprint.json`) and authenticates cloud sessions via **Google Sign-In (Jetpack Credential Manager)**.

- Sign in with Google from `WelcomeScreen` to connect live multi-device Firestore sync.
- Firestore rules enforce authenticated access (`request.auth != null`) across user-scoped and operational collections.

### Gemini API Key (Secrets Gradle Plugin)

API keys are managed using the **Secrets Gradle Plugin** via `.env` (or the **Secrets** panel in AI Studio) and exposed safely through `BuildConfig`:

```env
GEMINI_API_KEY=your_gemini_api_key_here
```

> **Offline Resilience:** When `GEMINI_API_KEY` is blank or unset, `AiAssistantScreen` automatically falls back to deterministic local knowledge & telemetry generators in `RagKnowledgeEngine` across all three roles (Citizen, Staff, and Admin).

---

## Authentication & RBAC Portals

QEase starts **unauthenticated** on `WelcomeScreen` and enforces strict **Role-Based Access Control (RBAC)** session isolation:

- **No in-app role switching:** Users must explicitly **Sign Out** (via confirmation modal) and authenticate from `WelcomeScreen` to enter a different portal.
- **Role-pure route guards:** `QEaseViewModel.setNavTab` blocks cross-role navigation so Staff and Admin sessions never leak Citizen discovery screens, and Citizens cannot access Staff/Admin consoles.
- **Assigned Counter Lock:** Staff accounts authenticate directly into their Admin-assigned service centre and counter number.

### Portal Credentials (Demo & Verification)

| Portal | Auth Method | Required Fields / Demo Credentials |
|--------|------------|-------------------------------------|
| **Citizen Portal** | Indian e-Seva **Mobile + OTP** | **Full Name \*** (min 2 chars) + **+91 10-digit Mobile** (`6–9` prefix) → **6-digit OTP**<br>Demo autofill: `Dr. Ananya Sharma` · `+91 9876543210` · OTP `482910` |
| **Staff Counter Desk** | Officer ID + Desk PIN | Officer ID: `rajesh.k@passport.gov.in` · Desk PIN: `1234` |
| **System Admin Console** | Admin ID + Master Key | Admin ID: `admin@qease.gov.in` · Master Key: `8899` |

---

## Role-Pure Screens & Navigation

Each role sees a tailored navigation bar (bottom bar on phones, side `NavigationRail` on medium/expanded screens):

### Citizen Portal (`UserRole.CUSTOMER` — 6 Tabs)

| Tab | Screen | Purpose |
|-----|--------|---------|
| **Home** | `CustomerHomeScreen` | Active token hero card, quick intelligence shortcuts, profile sign-out trigger |
| **Centres** | `CentresScreen` | Search/filter service centres, side-by-side comparison, submit live crowd reports |
| **Live Radar** | `WhereIsMyTurnScreen` | Counter-by-counter live radar timeline & community status updates |
| **Queue** | `MyQueueScreen` | Digital virtual token pass, ahead count, turn alerts & cancel action |
| **Reviews** | `ReviewsFeedbackScreen` | Citizen star ratings, speed breakdowns, helpfulness votes & review submission |
| **AI Help** | `AiAssistantScreen` | Grounded document checklist & service eligibility assistant |

### Staff Counter Desk (`UserRole.STAFF` — 4 Tabs)

`Live Radar`, `Centres`, and `Reviews` are removed from Staff navigation to keep the desk focused on real-time counter execution:

| Tab | Screen | Purpose |
|-----|--------|---------|
| **Desk** | `StaffDeskScreen` | Locked assigned counter control (Active/Paused), Call Next, Mark Serving, Complete, Re-queue, No-Show, Walk-In Token issuance & FIFO waiting queue |
| **Proposals** | `StaffProposalsScreen` | Two-segment workspace (`Service Info` and `Required Docs`) to propose fee, duration, caution, or checklist updates held for Admin review |
| **Alerts** | `StaffAlertsScreen` | Live unread badge for incoming queue/approval notifications + citizen crowd report verification (`100%` confidence boost) |
| **AI Copilot** | `AiAssistantScreen` | Desk-aware AI Copilot for SOP rules and one-tap proposal drafting |

### System Admin Console (`UserRole.ADMIN` — 5 Tabs)

Citizen `Services`, `Centres`, `Feedback`, and `Live Radar` tabs are removed in favor of dedicated governance operations:

| Tab | Screen | Purpose |
|-----|--------|---------|
| **Monitor** | `AdminMonitorScreen` | Real-time queue load across all centres, live counter states, maintenance alerts & Live Facility Notice broadcaster |
| **Approvals** | `AdminApprovalsScreen` | Live pending badge + structured Before/Proposed diff cards to `Approve & Publish` or `Reject` Staff edits |
| **Ops** | `AdminOpsScreen` | Three-segment configuration (`Staff` roster/PINs/counter assignment, `Centres` virtual queue & service toggles, and global `Settings`) |
| **Analytics** | `AdminAnalyticsScreen` | Live KPI snapshot (completion rate, avg wait, active counters), hourly token arrival trends & immutable audit trail |
| **AI** | `AiAssistantScreen` | Governance & Audit AI summarizing pending proposals, bottlenecks & system policies |

---

## Role-Aware AI Assistant & Smart Actions

`AiAssistantScreen`, `QEaseRepository.sendChatMessage`, `GeminiApiClient`, and `RagKnowledgeEngine` dynamically adapt system prompts, quick-prompt chips, live telemetry grounding, and interactive workflow buttons (`AiWorkflowAction`) per role:

- **Citizen (`Service Intelligence AI`):** Grounds responses in official document dossiers and triggers `OPEN_SERVICE_CHECKLIST` to jump straight to verified checklists.
- **Staff (`Counter Desk AI Copilot`):** Grounded in the officer's locked centre/counter queue load and unverified crowd reports, with one-tap action pills to:
  - `PREFILL_STAFF_SERVICE_PROPOSAL` — opens `Proposals → Service Info` with pre-populated fee, duration, caution note, and rationale.
  - `PREFILL_STAFF_DOC_PROPOSAL` — opens `Proposals → Required Docs` with a prefilled document addition draft.
  - `VERIFY_LATEST_CROWD_REPORT` / `OPEN_STAFF_ALERTS` — verifies citizen crowd reports or opens Desk Alerts.
  - `OPEN_STAFF_DESK` — returns directly to the active counter queue.
- **Admin (`Governance & Audit AI`):** Grounded in cross-centre bottlenecks, pending proposals, settings, and recent audit logs, with one-tap deep links to `OPEN_ADMIN_APPROVALS`, `OPEN_ADMIN_OPS_STAFF`, `OPEN_ADMIN_OPS_CENTRES`, `OPEN_ADMIN_OPS_SETTINGS`, and `OPEN_ADMIN_ANALYTICS`.

---

## Staff Proposal & Admin Approval Workflow

1. **Submit Proposal (Staff):** From `StaffProposalsScreen` (or via an AI Copilot prefill), a Staff officer submits a `SERVICE_INFO` or `REQUIRED_DOCUMENT_*` proposal with a mandatory rationale.
2. **Held Pending (`ProposalStatus.PENDING`):** Citizen-facing `services` remain untouched while the change awaits review; Admins receive an unread notification and pending badge increment.
3. **Approve or Reject (Admin):** From `AdminApprovalsScreen`, an Admin inspects the structured diff and either **Approves & Publishes** (immediately updating the live citizen service catalog and notifying Staff) or **Rejects** with review notes.

---

## Interactive Home Screen Widgets

- **Staff Counter Desk Widget (`StaffDeskWidgetProvider`):** Displays assigned counter status, active serving/called token, waiting queue count, and unread desk alerts. Supports one-tap **Call Next** action directly from the home screen or deep-linking into `STAFF_DESK` / `STAFF_ALERTS`.
- **Admin Governance Widget (`AdminMonitorWidgetProvider`):** Displays cross-centre waiting volume, active/total counters, and pending proposal count with deep-links into `ADMIN_MONITOR` and `ADMIN_APPROVALS`.
- **Snapshot Sync (`WidgetStateBridge`):** Automatically refreshes widget snapshots whenever queues, counters, proposals, or notifications change.

---

## Core Intelligence Engines

All domain intelligence lives in pure Kotlin engines under `domain/engine/`:

1. **`QueueStateMachine.kt`** — Enforces deterministic token lifecycle transitions (`WAITING` → `CALLED` → `SERVING` → `COMPLETED` / `NO_SHOW` / `CANCELLED`), preventing invalid state jumps or duplicate counter calls.
2. **`WaitTimeEstimator.kt`** — Calculates wait-time windows using tokens ahead, active counters, rolling service duration, time-of-day congestion factor, and verification complexity.
3. **`CrowdConfidenceEngine.kt`** — Aggregates community crowd reports using exponential time-decay (`t½ = 30 min`) with High/Medium/Low confidence badges, plus instant `100%` boost upon Staff verification.
4. **`RagKnowledgeEngine.kt`** — Grounds Citizen answers in verified service document dossiers and provides deterministic Staff Copilot and Admin Audit summaries when offline.

---

## Database & Persistence (Firestore + Room)

QEase pairs **Firebase Firestore** cloud sync (`FirebaseCloudSync`) with a local **Room (`QEaseDatabase`)** cache:

- **Firestore Collections:** Service centres, counters, queue tokens, services, staff accounts, change proposals, notifications, system settings, crowd reports, reviews, live queue updates, and audit logs.
- **Room Local Tables (`QEaseDatabase`):** `saved_centres`, `user_tokens`, `crowd_reports_local`, `centre_reviews`, `live_queue_updates`, and `user_accounts`.

---

## Adaptive & Responsive Design

Built around `QEaseLayoutMetrics` (`ResponsiveLayout.kt`) for **Compact (<600dp)**, **Medium (600–839dp)**, **Expanded (840dp+)** screens and **100%–125%+ font/display scaling**:

- **Adaptive Navigation:** Role-pure bottom bar (horizontally scrollable under tight widths or scaled displays) on phones; persistent side `NavigationRail` on wide screens.
- **Adaptive Grids & Two-Pane Desk:** `AdaptiveGridRow` scales from 1 to 2+ columns cleanly, and `StaffDeskScreen` switches to a side-by-side Desk Controls + Waiting Queue layout on expanded screens.
- **Accessibility & Styling:** `48.dp` minimum touch targets, Material icons throughout (no emojis or gradients), `ResponsiveDialogShell` with vertical scroll, and edge-to-edge `SystemBarStyle.light` insets.

---

## Testing & Verification

```bash
# Run full unit, widget & Robolectric Compose UI test suite
gradle :app:testDebugUnitTest

# Run Roborazzi screenshot verification
gradle :app:verifyRoborazziDebug
```

Test coverage includes:
- **`StaffAdminApiAndWidgetTest.kt`:** Staff proposal hold-until-Admin-approval lifecycle, Admin staff/counter creation, maintenance mode booking gate, Staff/Admin AppWidget `RemoteViews` updates, role-pure navigation verification (4 Staff tabs, 5 Admin tabs, 6 Citizen tabs), RBAC cross-role tab blocking, and Staff AI Copilot prefill / Admin AI Audit routing.
- **`ResponsiveUiRobolectricTest.kt`:** `QEaseLayoutMetrics` breakpoints across Compact/Medium/Expanded and `1.25f` font scaling, unauthenticated login gate, Citizen OTP validation, logout confirmation flow, markdown-clean AI chat rendering, and 125%-scale composition of all Citizen, Staff, and Admin screens.
- **`firestore.test.js`:** Security rules verification for Firestore access controls.

---

## Common Issues

### Citizen OTP request rejected during Sign-In

**Cause:** Citizen Full Name was left blank or under 2 characters, or the mobile number is not a valid 10-digit Indian number (`6–9` prefix).  
**Fix:** Enter your full name (at least 2 characters) and a valid 10-digit mobile number (or tap the demo citizen autofill chip).

### Gemini AI uses local fallback responses instead of live cloud responses

**Cause:** `GEMINI_API_KEY` is missing or unset in `.env` / AI Studio Secrets.  
**Fix:** Add a valid `GEMINI_API_KEY` to `.env` (or configure it in the AI Studio Secrets panel) and rebuild. Deterministic local RAG and telemetry answers continue working automatically without a key.

### Role tabs blocked or hidden inside a session

**Cause:** Strict RBAC session isolation prevents cross-role switching inside an active session and hides Citizen discovery tabs (`Live Radar`, `Centres`, `Reviews`) from Staff and Admin navigation.  
**Fix:** Tap the Sign Out icon in the top bar (or profile avatar on Citizen Home / Logout on wide `NavigationRail`), confirm sign-out, and log into the target portal from `WelcomeScreen`.

---

## Contributing

1. Fork the repository and branch off `main`: `git checkout -b feature/your-feature`
2. Follow MVVM + Jetpack Compose Material 3 guidelines and keep interactive touch targets >= `48.dp`.
3. Verify compilation and tests locally:
   ```bash
   gradle assembleDebug
   gradle :app:testDebugUnitTest
   ```
4. Open a pull request against `main`.

Maintained by **Vikash Gupta** — GitHub: [@Vortex-16](https://github.com/Vortex-16)

---

*Last updated: November 2026 · QEase Android Platform*
