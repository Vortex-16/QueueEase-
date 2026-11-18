package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class StaffWidgetSnapshot(
    val centreId: String = "centre_central_psk",
    val centreName: String = "Central Passport Seva Kendra (PSK)",
    val counterId: String = "cnt_psk_1",
    val counterNumber: Int = 1,
    val counterName: String = "Counter 1 - Token Verification & Intake",
    val assignedStaffName: String = "Officer Rajesh Kumar",
    val isCounterActive: Boolean = true,
    val currentTokenId: String = "",
    val currentTokenNumber: String = "",
    val currentTokenCitizen: String = "",
    val currentTokenService: String = "",
    val currentTokenStatus: String = "",
    val waitingCount: Int = 4,
    val unreadNotificationsCount: Int = 2,
    val lastUpdatedLabel: String = "Live"
)

data class AdminWidgetSnapshot(
    val totalWaitingCount: Int = 4,
    val activeCountersCount: Int = 7,
    val totalCountersCount: Int = 7,
    val pendingProposalsCount: Int = 2,
    val openCentresCount: Int = 5,
    val totalCentresCount: Int = 5,
    val maintenanceMode: Boolean = false,
    val lastUpdatedLabel: String = "Live"
)

object WidgetStateBridge {

    private const val PREFS_NAME = "qease_widget_bridge_prefs"

    const val EXTRA_WIDGET_DESTINATION = "qease_extra_widget_destination"
    const val DEST_STAFF_DESK = "STAFF_DESK"
    const val DEST_STAFF_ALERTS = "STAFF_ALERTS"
    const val DEST_ADMIN_MONITOR = "ADMIN_MONITOR"
    const val DEST_ADMIN_APPROVALS = "ADMIN_APPROVALS"

    private fun timeLabel(): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }

    fun saveStaffSnapshot(context: Context, snapshot: StaffWidgetSnapshot) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("staff_centre_id", snapshot.centreId)
            .putString("staff_centre_name", snapshot.centreName)
            .putString("staff_counter_id", snapshot.counterId)
            .putInt("staff_counter_num", snapshot.counterNumber)
            .putString("staff_counter_name", snapshot.counterName)
            .putString("staff_officer_name", snapshot.assignedStaffName)
            .putBoolean("staff_counter_active", snapshot.isCounterActive)
            .putString("staff_token_id", snapshot.currentTokenId)
            .putString("staff_token_num", snapshot.currentTokenNumber)
            .putString("staff_token_citizen", snapshot.currentTokenCitizen)
            .putString("staff_token_service", snapshot.currentTokenService)
            .putString("staff_token_status", snapshot.currentTokenStatus)
            .putInt("staff_waiting_count", snapshot.waitingCount)
            .putInt("staff_unread_alerts", snapshot.unreadNotificationsCount)
            .putString("staff_updated_at", timeLabel())
            .apply()
    }

    fun readStaffSnapshot(context: Context): StaffWidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return StaffWidgetSnapshot(
            centreId = prefs.getString("staff_centre_id", "centre_central_psk") ?: "centre_central_psk",
            centreName = prefs.getString("staff_centre_name", "Central Passport Seva Kendra (PSK)") ?: "Central Passport Seva Kendra (PSK)",
            counterId = prefs.getString("staff_counter_id", "cnt_psk_1") ?: "cnt_psk_1",
            counterNumber = prefs.getInt("staff_counter_num", 1),
            counterName = prefs.getString("staff_counter_name", "Counter 1 - Token Verification & Intake") ?: "Counter 1",
            assignedStaffName = prefs.getString("staff_officer_name", "Officer Rajesh Kumar") ?: "Officer Rajesh Kumar",
            isCounterActive = prefs.getBoolean("staff_counter_active", true),
            currentTokenId = prefs.getString("staff_token_id", "") ?: "",
            currentTokenNumber = prefs.getString("staff_token_num", "") ?: "",
            currentTokenCitizen = prefs.getString("staff_token_citizen", "") ?: "",
            currentTokenService = prefs.getString("staff_token_service", "") ?: "",
            currentTokenStatus = prefs.getString("staff_token_status", "") ?: "",
            waitingCount = prefs.getInt("staff_waiting_count", 4),
            unreadNotificationsCount = prefs.getInt("staff_unread_alerts", 2),
            lastUpdatedLabel = prefs.getString("staff_updated_at", "Live") ?: "Live"
        )
    }

    fun saveAdminSnapshot(context: Context, snapshot: AdminWidgetSnapshot) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("admin_waiting_count", snapshot.totalWaitingCount)
            .putInt("admin_active_counters", snapshot.activeCountersCount)
            .putInt("admin_total_counters", snapshot.totalCountersCount)
            .putInt("admin_pending_proposals", snapshot.pendingProposalsCount)
            .putInt("admin_open_centres", snapshot.openCentresCount)
            .putInt("admin_total_centres", snapshot.totalCentresCount)
            .putBoolean("admin_maintenance_mode", snapshot.maintenanceMode)
            .putString("admin_updated_at", timeLabel())
            .apply()
    }

    fun readAdminSnapshot(context: Context): AdminWidgetSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return AdminWidgetSnapshot(
            totalWaitingCount = prefs.getInt("admin_waiting_count", 4),
            activeCountersCount = prefs.getInt("admin_active_counters", 7),
            totalCountersCount = prefs.getInt("admin_total_counters", 7),
            pendingProposalsCount = prefs.getInt("admin_pending_proposals", 2),
            openCentresCount = prefs.getInt("admin_open_centres", 5),
            totalCentresCount = prefs.getInt("admin_total_centres", 5),
            maintenanceMode = prefs.getBoolean("admin_maintenance_mode", false),
            lastUpdatedLabel = prefs.getString("admin_updated_at", "Live") ?: "Live"
        )
    }

    fun notifyAllWidgets(context: Context) {
        val appContext = context.applicationContext
        val manager = runCatching { AppWidgetManager.getInstance(appContext) }.getOrNull() ?: return

        val staffComponent = ComponentName(appContext, StaffDeskWidgetProvider::class.java)
        val staffIds = runCatching { manager.getAppWidgetIds(staffComponent) }.getOrNull()
        if (staffIds != null && staffIds.isNotEmpty()) {
            val intent = Intent(appContext, StaffDeskWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, staffIds)
            }
            appContext.sendBroadcast(intent)
        }

        val adminComponent = ComponentName(appContext, AdminMonitorWidgetProvider::class.java)
        val adminIds = runCatching { manager.getAppWidgetIds(adminComponent) }.getOrNull()
        if (adminIds != null && adminIds.isNotEmpty()) {
            val intent = Intent(appContext, AdminMonitorWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, adminIds)
            }
            appContext.sendBroadcast(intent)
        }
    }
}
