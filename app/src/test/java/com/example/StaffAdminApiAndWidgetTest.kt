package com.example

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.LocalQEaseApiEngine
import com.example.data.model.AiActionType
import com.example.data.model.AiWorkflowAction
import com.example.data.model.ProposalStatus
import com.example.data.model.ProposalType
import com.example.data.model.TokenStatus
import com.example.data.model.UserRole
import com.example.ui.screens.AdminOpsScreen
import com.example.ui.screens.StaffProposalsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.QEaseViewModel
import com.example.widget.AdminMonitorWidgetProvider
import com.example.widget.StaffDeskWidgetProvider
import com.example.widget.WidgetStateBridge
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StaffAdminApiAndWidgetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var app: Application

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        LocalQEaseApiEngine.getInstance(app).resetToSeedDefaultsForTesting()
    }

    @Test
    fun `staff proposals are held pending until admin approves and publishes live`() {
        val vm = QEaseViewModel(app)
        val staffRes = vm.authenticateUser("rajesh.k@passport.gov.in", "1234", UserRole.STAFF)
        assertTrue(staffRes.isSuccess)

        val beforeService = vm.services.value.first { it.id == "srv_aadhaar_update" }
        val originalFee = beforeService.officialFee

        // Staff submits a service info update proposal
        val propRes = vm.submitServiceInfoProposal(
            serviceId = "srv_aadhaar_update",
            proposedFee = "$2.00 (Biometric Update)",
            proposedDurationMin = 12,
            proposedDescription = beforeService.description,
            proposedEligibility = beforeService.eligibility,
            proposedCautionNote = "Carry registered mobile for instant OTP",
            rationale = "Updated UIDAI fee schedule effective today",
            assignedCounterNumber = 1
        )
        assertTrue(propRes.isSuccess)
        val proposal = propRes.getOrThrow()
        assertEquals(ProposalStatus.PENDING, proposal.status)

        // Live catalog must NOT change while proposal is PENDING
        assertEquals(originalFee, vm.services.value.first { it.id == "srv_aadhaar_update" }.officialFee)

        // Switch cleanly to Admin after logout
        vm.confirmLogout()
        val adminRes = vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN)
        assertTrue(adminRes.isSuccess)

        // Admin approves proposal
        val approveRes = vm.approveProposal(proposal.id, "Verified against latest circular")
        assertTrue(approveRes.isSuccess)
        assertEquals(ProposalStatus.APPROVED, approveRes.getOrThrow().status)

        // Now live citizen catalog reflects updated fee & duration
        val updatedService = vm.services.value.first { it.id == "srv_aadhaar_update" }
        assertEquals("$2.00 (Biometric Update)", updatedService.officialFee)
        assertEquals(12, updatedService.avgProcessingDurationMin)

        // Staff receives approval notification
        assertTrue(
            vm.notifications.value.any {
                it.targetRole == UserRole.STAFF && it.title.contains("Approved", ignoreCase = true)
            }
        )
    }

    @Test
    fun `admin staff creation enables new locked counter login and maintenance blocks booking`() {
        val vm = QEaseViewModel(app)
        assertTrue(vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN).isSuccess)

        val newStaffRes = vm.createOrUpdateStaffAccount(
            existingId = null,
            name = "Officer Meera Nair",
            email = "meera.nair@rto.gov.in",
            phone = "+91 9811007722",
            assignedCentreId = "centre_metro_rto",
            assignedCounterId = "cnt_rto_2",
            department = "Vehicle RC Verification",
            pin = "5678",
            isActive = true
        )
        assertTrue(newStaffRes.isSuccess)

        // Logout Admin and sign in as newly created Staff Officer
        vm.confirmLogout()
        val staffLogin = vm.authenticateUser("meera.nair@rto.gov.in", "5678", UserRole.STAFF)
        assertTrue(staffLogin.isSuccess)
        assertEquals("centre_metro_rto", vm.selectedStaffCentreId.value)
        assertEquals("cnt_rto_2", vm.selectedStaffCounterId.value)

        // Admin enables Maintenance Mode -> Citizen joinQueue must block gracefully
        vm.confirmLogout()
        assertTrue(vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN).isSuccess)
        val settingsRes = vm.updateSystemSettings(
            vm.systemSettings.value.copy(maintenanceMode = true, maintenanceBanner = "Overnight sync active")
        )
        assertTrue(settingsRes.isSuccess)

        vm.confirmLogout()
        val citizenJoin = vm.repository.joinQueue("centre_central_psk", "srv_passport_renew", "Test Citizen", "9876543210")
        assertTrue(citizenJoin.isFailure)
    }

    @Test
    fun `staff queue actions synchronize staff and admin widget snapshots and render providers`() {
        val engine = LocalQEaseApiEngine.getInstance(app)
        val callRes = engine.callNextCustomer("cnt_psk_1", "Officer Rajesh Kumar")
        assertTrue(callRes.isSuccess)
        val called = callRes.getOrThrow()
        assertEquals(TokenStatus.CALLED, called.status)

        val staffSnap = WidgetStateBridge.readStaffSnapshot(app)
        assertEquals(called.tokenNumber, staffSnap.currentTokenNumber)
        assertEquals(TokenStatus.CALLED.name, staffSnap.currentTokenStatus)

        val adminSnap = WidgetStateBridge.readAdminSnapshot(app)
        assertTrue(adminSnap.pendingProposalsCount >= 2)
        assertTrue(adminSnap.activeCountersCount > 0)

        // Ensure widget RemoteViews rendering and provider updates run cleanly without exceptions
        val staffViews = StaffDeskWidgetProvider.buildRemoteViews(app)
        val adminViews = AdminMonitorWidgetProvider.buildRemoteViews(app)
        assertNotNull(staffViews)
        assertNotNull(adminViews)
        val inflatedStaff = staffViews.apply(app, android.widget.FrameLayout(app))
        val inflatedAdmin = adminViews.apply(app, android.widget.FrameLayout(app))
        assertEquals(
            called.tokenNumber,
            inflatedStaff.findViewById<android.widget.TextView>(R.id.tv_staff_token_number).text.toString()
        )
        assertEquals(
            adminSnap.pendingProposalsCount.toString(),
            inflatedAdmin.findViewById<android.widget.TextView>(R.id.tv_admin_approvals_value).text.toString()
        )

        val manager = AppWidgetManager.getInstance(app)
        val shadowManager = org.robolectric.Shadows.shadowOf(manager)
        val staffWidgetId = shadowManager.createWidget(
            StaffDeskWidgetProvider::class.java,
            R.layout.widget_staff_desk
        )
        val adminWidgetId = shadowManager.createWidget(
            AdminMonitorWidgetProvider::class.java,
            R.layout.widget_admin_monitor
        )
        StaffDeskWidgetProvider.updateStaffWidget(app, manager, staffWidgetId)
        AdminMonitorWidgetProvider.updateAdminWidget(app, manager, adminWidgetId)
    }

    @Test
    fun `staff proposals screen switches between service info and required docs`() {
        val vm = QEaseViewModel(app)
        vm.authenticateUser("rajesh.k@passport.gov.in", "1234", UserRole.STAFF)

        composeTestRule.setContent {
            MyApplicationTheme {
                StaffProposalsScreen(viewModel = vm)
            }
        }
        composeTestRule.onNodeWithTag("staff_proposals_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_propose_service_edit").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("staff_subtab_docs").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_propose_doc_add").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin ops screen switches across staff centres and settings subtabs`() {
        val vm = QEaseViewModel(app)
        vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN)

        composeTestRule.setContent {
            MyApplicationTheme {
                AdminOpsScreen(viewModel = vm)
            }
        }
        composeTestRule.onNodeWithTag("admin_ops_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_add_staff_officer").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_subtab_centres").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_add_service_centre").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("admin_subtab_settings").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("btn_save_system_settings").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `role-pure navigation removes live radar centres and reviews from staff and admin while retaining them for citizens`() {
        val vm = QEaseViewModel(app)
        composeTestRule.setContent {
            MyApplicationTheme {
                QEaseMainApp(vm)
            }
        }

        // Staff Login -> 4 role-pure tabs: Desk, Proposals, Alerts, AI Copilot
        vm.authenticateUser("rajesh.k@passport.gov.in", "1234", UserRole.STAFF)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_desk").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_proposals").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_alerts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai_copilot").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_live_radar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_centres").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_reviews").assertDoesNotExist()

        // Navigate across Staff top-level tabs
        composeTestRule.onNodeWithTag("nav_tab_proposals").performClick()
        composeTestRule.onNodeWithTag("staff_proposals_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_alerts").performClick()
        composeTestRule.onNodeWithTag("staff_alerts_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai_copilot").performClick()
        composeTestRule.onNodeWithTag("ai_assistant_screen").assertIsDisplayed()

        // Admin Login -> 5 role-pure tabs: Monitor, Approvals, Ops, Analytics, AI
        vm.confirmLogout()
        vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_monitor").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_approvals").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ops").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_analytics").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_live_radar").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_centres").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_feedback").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_tab_services").assertDoesNotExist()

        // Navigate across Admin top-level tabs
        composeTestRule.onNodeWithTag("nav_tab_approvals").performClick()
        composeTestRule.onNodeWithTag("admin_approvals_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ops").performClick()
        composeTestRule.onNodeWithTag("admin_ops_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_analytics").performClick()
        composeTestRule.onNodeWithTag("admin_analytics_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai").performClick()
        composeTestRule.onNodeWithTag("ai_assistant_screen").assertIsDisplayed()

        // Citizen Login -> Retains full Citizen discovery suite
        vm.confirmLogout()
        val otp = vm.requestCitizenOtp("9876543210", "Aarav Sharma").getOrThrow()
        assertTrue(vm.verifyCitizenOtp("9876543210", otp, "Aarav Sharma").isSuccess)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_tab_home").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_centres").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_live_radar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_queue").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_reviews").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_ai_help").assertIsDisplayed()
    }

    @Test
    fun `staff and admin ai workflow actions prefill proposals and open audit routes`() {
        val vm = QEaseViewModel(app)
        vm.authenticateUser("rajesh.k@passport.gov.in", "1234", UserRole.STAFF)

        composeTestRule.setContent {
            MyApplicationTheme {
                QEaseMainApp(vm)
            }
        }

        // Staff AI Copilot prefills a Service Info Update Proposal
        vm.executeAiWorkflowAction(
            AiWorkflowAction(
                type = AiActionType.PREFILL_STAFF_SERVICE_PROPOSAL,
                label = "Prefill Service Update Proposal",
                targetServiceId = "srv_passport_renew",
                prefillDurationMin = 11,
                prefillRationale = "Desk throughput optimization"
            )
        )
        composeTestRule.waitForIdle()
        assertEquals(MainNavTab.STAFF_PROPOSALS, vm.currentTab.value)
        composeTestRule.onNodeWithTag("staff_proposals_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("proposal_rationale_input").performScrollTo().assertIsDisplayed()

        // Staff cannot navigate to Citizen-only LIVE_TRACKER tab
        vm.setNavTab(MainNavTab.LIVE_TRACKER)
        assertNotEquals(MainNavTab.LIVE_TRACKER, vm.currentTab.value)

        // Admin AI Audit routes directly to Approvals and Ops Settings
        vm.confirmLogout()
        vm.authenticateUser("admin@qease.gov.in", "8899", UserRole.ADMIN)
        vm.executeAiWorkflowAction(
            AiWorkflowAction(
                type = AiActionType.OPEN_ADMIN_APPROVALS,
                label = "Review Pending Approvals"
            )
        )
        composeTestRule.waitForIdle()
        assertEquals(MainNavTab.ADMIN_APPROVALS, vm.currentTab.value)
        composeTestRule.onNodeWithTag("admin_approvals_screen").assertIsDisplayed()

        vm.executeAiWorkflowAction(
            AiWorkflowAction(
                type = AiActionType.OPEN_ADMIN_OPS_SETTINGS,
                label = "Open System Policy & Settings"
            )
        )
        composeTestRule.waitForIdle()
        assertEquals(MainNavTab.ADMIN_OPS, vm.currentTab.value)
        assertEquals(2, vm.adminSubTab.value)
        composeTestRule.onNodeWithTag("btn_save_system_settings").performScrollTo().assertIsDisplayed()
    }
}
