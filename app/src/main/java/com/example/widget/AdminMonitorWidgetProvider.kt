package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.api.LocalQEaseApiEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdminMonitorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAdminWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADMIN_SYNC) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val engine = LocalQEaseApiEngine.getInstance(context)
                    engine.syncWidgetsFromCurrentState()
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, AdminMonitorWidgetProvider::class.java))
                    onUpdate(context, manager, ids)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_ADMIN_SYNC = "com.example.widget.ACTION_ADMIN_SYNC"

        fun buildRemoteViews(context: Context): RemoteViews {
            val snap = WidgetStateBridge.readAdminSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_admin_monitor)

            val modeStatus = if (snap.maintenanceMode) "MAINTENANCE MODE" else "${snap.openCentresCount}/${snap.totalCentresCount} Centres Open"
            views.setTextViewText(R.id.tv_admin_widget_subtitle, "$modeStatus • ${snap.lastUpdatedLabel}")
            views.setTextViewText(R.id.tv_admin_waiting_value, snap.totalWaitingCount.toString())
            views.setTextViewText(R.id.tv_admin_counters_value, "${snap.activeCountersCount}/${snap.totalCountersCount}")
            views.setTextViewText(R.id.tv_admin_approvals_value, snap.pendingProposalsCount.toString())
            views.setTextViewText(
                R.id.btn_admin_review_changes,
                if (snap.pendingProposalsCount > 0) "Review Edits (${snap.pendingProposalsCount})" else "Approvals"
            )

            // Sync action
            val syncIntent = Intent(context, AdminMonitorWidgetProvider::class.java).apply {
                action = ACTION_ADMIN_SYNC
            }
            views.setOnClickPendingIntent(
                R.id.btn_admin_sync_action,
                PendingIntent.getBroadcast(
                    context,
                    201,
                    syncIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Deep-link to Admin Approvals
            val reviewIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetStateBridge.EXTRA_WIDGET_DESTINATION, WidgetStateBridge.DEST_ADMIN_APPROVALS)
            }
            views.setOnClickPendingIntent(
                R.id.btn_admin_review_changes,
                PendingIntent.getActivity(
                    context,
                    202,
                    reviewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Deep-link to Admin Live Monitor
            val monitorIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetStateBridge.EXTRA_WIDGET_DESTINATION, WidgetStateBridge.DEST_ADMIN_MONITOR)
            }
            val monitorPending = PendingIntent.getActivity(
                context,
                203,
                monitorIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_admin_open_monitor, monitorPending)
            views.setOnClickPendingIntent(R.id.widget_admin_root, monitorPending)

            return views
        }

        fun updateAdminWidget(
            context: Context,
            appWidgetManager: AppWidgetManager?,
            appWidgetId: Int
        ) {
            val views = buildRemoteViews(context)
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }
}
