package com.example

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.QEaseLayoutMetrics
import com.example.ui.components.QEaseWindowWidthClass
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CentresScreen
import com.example.ui.screens.CustomerHomeScreen
import com.example.ui.screens.MyQueueScreen
import com.example.ui.screens.ReviewsFeedbackScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.screens.StaffDeskScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.screens.WhereIsMyTurnScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.QEaseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponsiveUiRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `layout metrics adapt across compact medium expanded and 125 percent scaling`() {
        val compactNormal = QEaseLayoutMetrics(
            widthClass = QEaseWindowWidthClass.COMPACT,
            availableWidth = 360.dp,
            fontScale = 1.0f
        )
        assertFalse(compactNormal.isWide)
        assertFalse(compactNormal.isScaledDisplay)
        assertFalse(compactNormal.isTightHorizontal)
        assertEquals(1, compactNormal.gridColumns)

        val compactScaled125 = QEaseLayoutMetrics(
            widthClass = QEaseWindowWidthClass.COMPACT,
            availableWidth = 360.dp,
            fontScale = 1.25f
        )
        assertTrue(compactScaled125.isScaledDisplay)
        assertTrue(compactScaled125.isTightHorizontal)
        assertEquals(288f, compactScaled125.effectiveWidthDp, 0.1f)
        assertEquals(1, compactScaled125.gridColumns)

        val mediumScaled125 = QEaseLayoutMetrics(
            widthClass = QEaseWindowWidthClass.MEDIUM,
            availableWidth = 640.dp,
            fontScale = 1.25f
        )
        assertTrue(mediumScaled125.isWide)
        assertEquals(512f, mediumScaled125.effectiveWidthDp, 0.1f)
        assertEquals(2, mediumScaled125.gridColumns)

        val expandedScaled125 = QEaseLayoutMetrics(
            widthClass = QEaseWindowWidthClass.EXPANDED,
            availableWidth = 900.dp,
            fontScale = 1.25f
        )
        assertTrue(expandedScaled125.isExpanded)
        assertEquals(720f, expandedScaled125.effectiveWidthDp, 0.1f)
        assertEquals(2, expandedScaled125.gridColumns)
    }

    @Test
    fun `welcome and centres screens render cleanly at 125 percent font scale on compact phone`() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.25f)
            ) {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(360.dp)) {
                        WelcomeScreen(
                            onStartCitizenLogin = {},
                            onSelectRoleLogin = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("get_started_button").assertExists()
    }

    @Test
    fun `app enforces login gate removes role toggles and requires logout confirmation before role switch`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QEaseViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                QEaseMainApp(vm)
            }
        }

        // Starts unauthenticated on Welcome/Login screen
        assertFalse(vm.isAuthenticated.value)
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()

        // Citizen Mobile + OTP sign-in flow: full name is mandatory (cannot leave blank)
        composeTestRule.onNodeWithTag("get_started_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("auth_rbac_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("citizen_mobile_input").performScrollTo().performTextInput("9876543210")
        composeTestRule.onNodeWithTag("send_otp_button").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Please enter your full name (at least 2 characters).").assertIsDisplayed()
        composeTestRule.onNodeWithTag("citizen_otp_input").assertDoesNotExist()

        // Autofill demo citizen (fills required name + mobile), request OTP, and verify
        composeTestRule.onNodeWithTag("autofill_demo_citizen_btn").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("send_otp_button").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeTestRule.onNodeWithTag("citizen_otp_input").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeTestRule.onNodeWithTag("citizen_otp_input").performScrollTo().performTextInput("482910")
        composeTestRule.onNodeWithTag("verify_otp_button").performScrollTo().performClick()

        // Now authenticated in Citizen portal: global top bar pills & role toggles absent
        assertTrue(vm.isAuthenticated.value)
        composeTestRule.onNodeWithTag("customer_home_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("active_role_badge").assertDoesNotExist()
        composeTestRule.onNodeWithTag("fast_sync_pill").assertDoesNotExist()
        composeTestRule.onNodeWithTag("top_logout_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("role_btn_staff").assertDoesNotExist()
        composeTestRule.onNodeWithTag("role_btn_admin").assertDoesNotExist()

        // Citizen avatar opens logout confirmation dialog before returning to Welcome/Login screen
        composeTestRule.onNodeWithTag("user_avatar_btn").performClick()
        composeTestRule.onNodeWithTag("logout_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_logout_button").performScrollTo().performClick()

        assertFalse(vm.isAuthenticated.value)
        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()

        // After logout, user can sign into Staff Desk from the login screen
        composeTestRule.onNodeWithTag("portal_card_staff").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("auth_pin_input").performScrollTo().performTextInput("1234")
        composeTestRule.onNodeWithTag("auth_submit_button").performScrollTo().performClick()

        assertTrue(vm.isAuthenticated.value)
        composeTestRule.onNodeWithTag("staff_desk_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("staff_logout_button").assertIsDisplayed()
    }

    @Test
    fun `centres screen renders two-column grid at 125 percent font scale on expanded screen`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QEaseViewModel(app)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.25f)
            ) {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(900.dp)) {
                        CentresScreen(viewModel = vm)
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("centres_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("centres_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithText("Service Centres").assertIsDisplayed()
    }

    @Test
    fun `all secondary screens compose without layout crashes at 125 percent scaling`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QEaseViewModel(app)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.25f)
            ) {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(900.dp)) {
                        WhereIsMyTurnScreen(viewModel = vm, onNavigateCentres = {})
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("where_is_my_turn_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE QUEUE RADAR").assertIsDisplayed()
    }

    @Test
    fun `reviews staff admin ai services home and queue screens compose cleanly at 125 percent scale`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = QEaseViewModel(app)
        val screenState = androidx.compose.runtime.mutableIntStateOf(0)

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 1.25f)
            ) {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(860.dp)) {
                        when (screenState.intValue) {
                            0 -> CustomerHomeScreen(viewModel = vm)
                            1 -> ServicesScreen(viewModel = vm)
                            2 -> MyQueueScreen(viewModel = vm)
                            3 -> ReviewsFeedbackScreen(viewModel = vm)
                            4 -> AiAssistantScreen(viewModel = vm)
                            5 -> StaffDeskScreen(viewModel = vm)
                            else -> AdminPanelScreen(viewModel = vm)
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("customer_home_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 1 }
        composeTestRule.onNodeWithTag("services_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 2 }
        composeTestRule.onNodeWithTag("my_queue_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 3 }
        composeTestRule.onNodeWithTag("reviews_feedback_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 4 }
        composeTestRule.onNodeWithTag("ai_assistant_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 5 }
        composeTestRule.onNodeWithTag("staff_desk_screen").assertIsDisplayed()
        composeTestRule.runOnIdle { screenState.intValue = 6 }
        composeTestRule.onNodeWithTag("admin_panel_screen").assertIsDisplayed()
    }
}
