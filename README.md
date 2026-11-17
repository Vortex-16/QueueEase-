# QEase — Service & Queue Intelligence

> Native Android application built with **Kotlin + Jetpack Compose (Material Design 3)** for smart virtual queuing, live turn tracking, grounded document readiness, community crowd intelligence, and role-gated counter operations.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Project Structure](#project-structure)
3. [Getting Started](#getting-started)
4. [Environment Variables & Secrets](#environment-variables--secrets)
5. [Authentication & RBAC Portals](#authentication--rbac-portals)
6. [Core Intelligence Engines](#core-intelligence-engines)
7. [Database & Persistence (Room)](#database--persistence-room)
8. [Screens & Navigation](#screens--navigation)
9. [Adaptive & Responsive Design](#adaptive--responsive-design)
10. [Staff Desk & Admin Consoles](#staff-desk--admin-consoles)
11. [Testing & Verification](#testing--verification)
12. [Common Issues](#common-issues)
13. [Contributing](#contributing)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI Framework | Jetpack Compose + Material Design 3 (`Professional Polish` theme) |
| Architecture | MVVM (`QEaseViewModel` + `StateFlow`) & Domain Engines |
| Local Database | AndroidX Room 2.7.0 + KSP (`QEaseDatabase`) |
| Networking & AI | Retrofit 2.12 + OkHttp + Moshi + Gemini Grounded RAG (`RagKnowledgeEngine`) |
| Async & Reactivity | Kotlin Coroutines + `StateFlow` / `collectAsStateWithLifecycle` |
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
├── gradle/
│   └── libs.versions.toml                      ← Version catalog for all dependencies
├── docs/
│   └── ARCHITECTURE_DECISIONS.md               ← System architecture, formulas & RBAC ADR
└── app/
    ├── build.gradle.kts                        ← App Gradle config, Secrets plugin & signing
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── drawable/
        │   │   │   ├── ic_launcher_background.xml
        │   │   │   ├── ic_launcher_foreground.xml
        │   │   │   └── ic_qease_minimal_logo.jpg   ← Minimalist brand & adaptive launcher asset
        │   │   └── values/
        │   │       ├── colors.xml
        │   │       ├── strings.xml
        │       └── themes.xml
        │   └── java/com/example/
        │       ├── MainActivity.kt             ← Edge-to-edge entry, login gate & adaptive nav shell
        │       ├── data/
        │       │   ├── local/
        │       │   │   ├── Entities.kt         ← Room entities (tokens, reviews, crowd, accounts)
        │       │   │   ├── QEaseDao.kt         ← Room DAO queries & reactive Flow streams
        │       │   │   └── QEaseDatabase.kt    ← Singleton Room database instance
        │       │   ├── model/
        │       │   │   └── Models.kt           ← Domain models, roles, counters, audit logs
        │       │   ├── remote/
        │       │   │   └── GeminiApiService.kt ← Retrofit + Moshi Gemini API client
        │       │   └── repository/
        │       │       └── QEaseRepository.kt  ← Single source of truth, RBAC auth & fast sync
        │       ├── domain/engine/
        │       │   ├── CrowdConfidenceEngine.kt ← Exponential time-decay crowd freshness scoring
        │       │   ├── QueueStateMachine.kt    ← Deterministic FIFO token state transitions
        │       │   ├── RagKnowledgeEngine.kt   ← Grounded service dossier & zero-hallucination RAG
        │       │   └── WaitTimeEstimator.kt    ← Multi-factor wait-time range estimator
        │       └── ui/
        │           ├── components/
        │           │   ├── AuthRbacDialog.kt   ← Role-locked Citizen OTP & Officer PIN modals
        │           │   ├── QEaseComponents.kt  ← Shared cards, badges, indicators & headers
        │           │   └── ResponsiveLayout.kt ← Window width classes, 125%+ scale metrics, DialogShell
        │           ├── screens/
        │           │   ├── WelcomeScreen.kt    ← Portal selector & onboarding landing screen
        │           │   ├── CustomerHomeScreen.kt ← Citizen dashboard, active token & shortcuts
        │           │   ├── CentresScreen.kt    ← Facility explorer, comparison & crowd reporting
        │           │   ├── ServicesScreen.kt   ← Service catalog & interactive document checklists
        │           │   ├── MyQueueScreen.kt    ← Virtual queue ticket, counter status & turn alerts
        │           │   ├── WhereIsMyTurnScreen.kt ← "Where is My Train" style live queue radar
        │           │   ├── ReviewsFeedbackScreen.kt ← Community reviews, speed ratings & helpful votes
        │           │   ├── AiAssistantScreen.kt ← Grounded Service Intelligence chat assistant
        │           │   ├── StaffDeskScreen.kt  ← Counter desk operator workflow & crowd verification
        │           │   └── AdminPanelScreen.kt ← System notices, service toggles & live audit trail
        │           ├── theme/
        │           │   ├── Color.kt            ← Professional Polish forest/sage color tokens
        │           │   ├── Theme.kt            ← Material 3 theme & system bar icon contrast
        │           │   └── Type.kt             ← Typography scale
        │           └── viewmodel/
        │               └── QEaseViewModel.kt   ← UI state, tab guards & RBAC session actions
        └── test/java/com/example/
            ├── ExampleUnitTest.kt
            ├── ExampleRobolectricTest.kt
            ├── GreetingScreenshotTest.kt       ← Roborazzi visual snapshot test
            └── ResponsiveUiRobolectricTest.kt  ← End-to-end RBAC, OTP, logout & 125% scale tests
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

# Copy the environment template (optional if using grounded local RAG fallback)
cp .env.example .env

# Build debug APK
gradle assembleDebug

# Run all unit & Robolectric UI tests
gradle :app:testDebugUnitTest
```

> **Note:** Use `gradle` directly or Android Studio to build. Never store API keys in `local.properties`.

---

## Environment Variables & Secrets

API keys are managed using the **Secrets Gradle Plugin** via `.env` (or the **Secrets** panel in AI Studio) and exposed safely through `BuildConfig`.

```env
GEMINI_API_KEY=your_gemini_api_key_here
```

> **Important:**
> - Never commit `.env` or hardcode API keys in Kotlin or Gradle source files.
> - When `GEMINI_API_KEY` is blank or unset, `AiAssistantScreen` automatically falls back to the deterministic local `RagKnowledgeEngine` grounded in official service checklists so the assistant remains 100% functional offline.

---

## Authentication & RBAC Portals

QEase starts **unauthenticated** on `WelcomeScreen` and enforces strict **Role-Based Access Control (RBAC)** session isolation:

- **No in-app role switching:** Cross-role toggles are removed from the workspace. To switch between Citizen, Staff Desk, and Admin accounts, a user must explicitly **Log Out** (with confirmation dialog) and re-authenticate from the Welcome & Login screen.
- **Role-locked modals:** Each portal card opens a dedicated modal locked to that role.

### Portal Credentials (Demo & Verification)

| Portal | Auth Method | Required Fields / Demo Credentials |
|--------|------------|-------------------------------------|
| **Citizen Portal** | Indian e-Seva Style **Mobile + OTP** | **Full Name \*** (mandatory, min 2 chars) + **+91 10-digit Mobile** (`6–9` prefix) → **6-digit OTP**<br>Demo autofill: `Dr. Ananya Sharma` · `+91 9876543210` · OTP `482910` |
| **Staff Counter Desk** | Officer ID + Desk PIN | Officer ID: `rajesh.k@passport.gov.in` · Desk PIN: `1234` |
| **System Admin Console** | Admin ID + Master Key | Admin ID: `admin@qease.gov.in` · Master Key: `8899` |

---

## Core Intelligence Engines

All domain intelligence lives in pure Kotlin engines under `domain/engine/`:

1. **`QueueStateMachine.kt`** — Enforces deterministic token lifecycle transitions (`WAITING` → `CALLED` → `SERVING` → `COMPLETED` / `NO_SHOW` / `CANCELLED`), preventing invalid state jumps or duplicate counter calls.
2. **`WaitTimeEstimator.kt`** — Calculates realistic wait-time windows (e.g., `18–25 min`) using tokens ahead, active counters, rolling service duration, time-of-day congestion factor, and verification complexity.
3. **`CrowdConfidenceEngine.kt`** — Aggregates community crowd reports using exponential time-decay (`t½ = 30 min`) with High/Medium/Low confidence badges, plus instant `100%` boost upon Staff verification.
4. **`RagKnowledgeEngine.kt`** — Grounds AI responses strictly in verified service document dossiers (required IDs, address proofs, forms, fees, eligibility) with prompt-injection resistance.

---

## Database & Persistence (Room)

QEase persists live user and facility data locally via **Room (`QEaseDatabase`)** and synchronises reactive state flows across portals:

| Room Entity | Table Name | Purpose |
|-------------|-----------|---------|
| `SavedCentreEntity` | `saved_centres` | Bookmarked service centres |
| `UserTokenEntity` | `user_tokens` | Citizen virtual queue tokens & position history |
| `OfflineCrowdReportEntity` | `crowd_reports_local` | Community-submitted crowd & wait-time reports |
| `CentreReviewEntity` | `centre_reviews` | Citizen star ratings, visit tags & queue speed feedback |
| `LiveQueueUpdateEntity` | `live_queue_updates` | Real-time "Where is My Turn" live counter updates & upvotes |
| `UserAccountEntity` | `user_accounts` | Authenticated account profiles & role metadata |

---

## Screens & Navigation

### Citizen Portal (`UserRole.CUSTOMER`)

| Tab / Screen | Purpose |
|-------------|---------|
| **Home** (`CustomerHomeScreen`) | Active token hero card, quick intelligence shortcuts, profile avatar logout trigger |
| **Centres** (`CentresScreen`) | Search/filter service centres, side-by-side comparison, submit live crowd reports |
| **Live Radar** (`WhereIsMyTurnScreen`) | "Where is My Train" style counter-by-counter station timeline & live status feed |
| **Queue** (`MyQueueScreen`) | Digital token pass, ahead count, turn alerts & leave/cancel queue actions |
| **Reviews** (`ReviewsFeedbackScreen`) | Citizen ratings, service speed breakdowns, helpfulness upvotes & new review submission |
| **AI Help** (`AiAssistantScreen`) | Grounded document checklist & service eligibility assistant |

### Staff & Admin Portals

- **Staff Desk (`StaffDeskScreen`)**: Open/close counters, atomically call next waiting token, mark `SERVING` / `COMPLETED` / `NO_SHOW`, verify citizen crowd reports to `100%` confidence, and sign out via the header action or navigation rail.
- **System Admin (`AdminPanelScreen`)**: Broadcast live facility notices to citizens, toggle service availability, inspect real-time RBAC audit logs, and sign out securely.

---

## Adaptive & Responsive Design

Built around `QEaseLayoutMetrics` (`ResponsiveLayout.kt`) to stay structured across **Compact (<600dp)**, **Medium (600–839dp)**, **Expanded (840dp+)** screens and **100%–125%+ font/display scaling**:

- **Adaptive Navigation**: Bottom navigation bar (`NavigationBar` style, horizontally scrollable under tight/125% scaling) on phones; persistent side **`NavigationRail`** on Medium/Expanded screens.
- **Adaptive Grids**: `AdaptiveGridRow` switches cleanly between 1-column and 2-column (or 4-column on expanded welcome cards) layouts using `IntrinsicSize.Max` for uniform card heights.
- **Overflow-Safe Headers & Modals**: Uses `FlowRow` wrapping, `ResponsiveDialogShell` with `verticalScroll`, `48.dp` minimum touch targets, and edge-to-edge `SystemBarStyle.light` insets for dark, legible system status bar icons.

---

## Testing & Verification

```bash
# Run full unit + Robolectric UI + responsive scaling test suite
gradle :app:testDebugUnitTest

# Run Roborazzi screenshot verification
gradle :app:verifyRoborazziDebug
```

Test coverage in `ResponsiveUiRobolectricTest.kt` verifies:
- `QEaseLayoutMetrics` breakpoints across Compact, Medium, Expanded, and `1.25f` font scaling.
- `WelcomeScreen`, `CentresScreen`, `WhereIsMyTurnScreen`, `CustomerHomeScreen`, `ServicesScreen`, `MyQueueScreen`, `ReviewsFeedbackScreen`, `AiAssistantScreen`, `StaffDeskScreen`, and `AdminPanelScreen` rendering at `125%` scale without crashes.
- Full RBAC & login gate flow: unauthenticated startup → mandatory Citizen Full Name validation → `+91` Mobile + OTP verification → absence of cross-role toggles → Logout confirmation dialog → Staff Desk PIN authentication.

---

## Common Issues

### ✗ `citizen_otp_input` or OTP request rejected during Citizen Sign-In

**Cause:** Citizen Full Name was left blank or under 2 characters, or the mobile number is not a valid 10-digit Indian number (`6–9` prefix).  
**Fix:** Enter your full name (at least 2 characters) and a valid 10-digit mobile number (or tap the demo citizen autofill chip).

---

### ✗ Gemini AI returns local fallback responses instead of live cloud responses

**Cause:** `GEMINI_API_KEY` is missing or commented out in `.env` / AI Studio Secrets.  
**Fix:** Add a valid `GEMINI_API_KEY` to `.env` (or configure it in the AI Studio Secrets panel) and rebuild. Grounded local RAG responses continue to work automatically without a key.

---

### ✗ Role tabs (`Staff Desk` / `Admin`) blocked when logged in as Citizen

**Cause:** Strict RBAC session isolation prevents cross-role switching inside an active session.  
**Fix:** Tap your profile avatar (or Logout icon in Staff/Admin top bar / wide NavigationRail), confirm sign-out, and log into the target portal from the Welcome & Login screen.

---

## Contributing

1. Fork the repository and branch off `main`: `git checkout -b feature/your-feature`
2. Follow MVVM + Jetpack Compose Material 3 guidelines and keep interactive touch targets ≥ `48.dp`.
3. Verify compilation and tests locally:
   ```bash
   gradle assembleDebug
   gradle :app:testDebugUnitTest
   ```
4. Open a pull request against `main`.

Maintained by **Vikash Gupta** — GitHub: [@Vortex-16](https://github.com/Vortex-16)

---

*Last updated: November 2026 · QEase Android Platform*
