package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.api.LocalQEaseApiEngine
import com.example.data.model.TokenStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StaffDeskWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateStaffWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_STAFF_PRIMARY_ACTION || action == ACTION_STAFF_TOGGLE_COUNTER) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val engine = LocalQEaseApiEngine.getInstance(context)
                    val snap = WidgetStateBridge.readStaffSnapshot(context)
                    when (action) {
                        ACTION_STAFF_TOGGLE_COUNTER -> {
                            engine.toggleCounterStatus(snap.counterId, snap.assignedStaffName)
                        }
                        ACTION_STAFF_PRIMARY_ACTION -> {
                            when (snap.currentTokenStatus) {
                                TokenStatus.CALLED.name -> engine.markServing(snap.currentTokenId, snap.assignedStaffName)
                                TokenStatus.SERVING.name -> engine.completeService(snap.currentTokenId, snap.assignedStaffName)
                                else -> engine.callNextCustomer(snap.counterId, snap.assignedStaffName)
                            }
                        }
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(ComponentName(context, StaffDeskWidgetProvider::class.java))
                    onUpdate(context, manager, ids)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_STAFF_PRIMARY_ACTION = "com.example.widget.ACTION_STAFF_PRIMARY_ACTION"
        const val ACTION_STAFF_TOGGLE_COUNTER = "com.example.widget.ACTION_STAFF_TOGGLE_COUNTER"

        fun buildRemoteViews(context: Context): RemoteViews {
            val snap = WidgetStateBridge.readStaffSnapshot(context)
            val views = RemoteViews(context.packageName, R.layout.widget_staff_desk)

            views.setTextViewText(R.id.tv_staff_widget_title, "QEase Staff • Counter ${snap.counterNumber}")
            views.setTextViewText(R.id.tv_staff_counter_scope, "${snap.centreName} • ${snap.lastUpdatedLabel}")

            views.setTextViewText(
                R.id.btn_staff_toggle_status,
                if (snap.isCounterActive) "ACTIVE" else "PAUSED"
            )
            views.setTextColor(
                R.id.btn_staff_toggle_status,
                if (snap.isCounterActive) Color.parseColor("#0F6B36") else Color.parseColor("#B3261E")
            )

            val hasActiveToken = snap.currentTokenNumber.isNotBlank()
            views.setTextViewText(
                R.id.tv_staff_token_number,
                if (hasActiveToken) snap.currentTokenNumber else "DESK IDLE"
            )
            views.setTextViewText(
                R.id.tv_staff_token_detail,
                if (hasActiveToken) {
                    "${snap.currentTokenCitizen} • ${snap.currentTokenStatus}"
                } else {
                    "Tap Call Next to serve next citizen"
                }
            )
            views.setTextViewText(R.id.tv_staff_waiting_badge, "${snap.waitingCount} Waiting")

            val primaryBtnLabel = when (snap.currentTokenStatus) {
                TokenStatus.CALLED.name -> "Start Serving"
                TokenStatus.SERVING.name -> "Complete"
                else -> if (snap.isCounterActive) "Call Next" else "Paused"
            }
            views.setTextViewText(R.id.btn_staff_primary_action, primaryBtnLabel)
            views.setTextViewText(
                R.id.btn_staff_alerts_action,
                if (snap.unreadNotificationsCount > 0) "Alerts (${snap.unreadNotificationsCount})" else "Alerts"
            )

            // Interactive Broadcast PendingIntents
            val primaryIntent = Intent(context, StaffDeskWidgetProvider::class.java).apply {
                action = ACTION_STAFF_PRIMARY_ACTION
            }
            views.setOnClickPendingIntent(
                R.id.btn_staff_primary_action,
                PendingIntent.getBroadcast(
                    context,
                    101,
                    primaryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val toggleIntent = Intent(context, StaffDeskWidgetProvider::class.java).apply {
                action = ACTION_STAFF_TOGGLE_COUNTER
            }
            views.setOnClickPendingIntent(
                R.id.btn_staff_toggle_status,
                PendingIntent.getBroadcast(
                    context,
                    102,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Deep-link Launch PendingIntents
            val openDeskIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetStateBridge.EXTRA_WIDGET_DESTINATION, WidgetStateBridge.DEST_STAFF_DESK)
            }
            val openDeskPending = PendingIntent.getActivity(
                context,
                103,
                openDeskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_staff_open_desk, openDeskPending)
            views.setOnClickPendingIntent(R.id.widget_staff_root, openDeskPending)

            val alertsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(WidgetStateBridge.EXTRA_WIDGET_DESTINATION, WidgetStateBridge.DEST_STAFF_ALERTS)
            }
            views.setOnClickPendingIntent(
                R.id.btn_staff_alerts_action,
                PendingIntent.getActivity(
                    context,
                    104,
                    alertsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            return views
        }

        fun updateStaffWidget(
            context: Context,
            appWidgetManager: AppWidgetManager?,
            appWidgetId: Int
        ) {
            val views = buildRemoteViews(context)
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }
}
