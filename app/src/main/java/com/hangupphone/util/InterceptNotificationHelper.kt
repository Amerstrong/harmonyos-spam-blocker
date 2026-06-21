/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: InterceptNotificationHelper.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 用于在电话拦截成功时发送系统通知的辅助工具类
 */
package com.hangupphone.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hangupphone.data.entity.InterceptReason
import com.hangupphone.ui.MainActivity

object InterceptNotificationHelper {

    private const val CHANNEL_ID = "intercept_channel"
    private const val NOTIFICATION_ID = 1001

    fun showBlocked(context: Context, number: String, reason: InterceptReason) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, nm)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("已拦截陌生来电")
            .setContentText("$number · ${reason.displayName}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "号码：$number\n原因：${reason.displayName}\n可在 HangupPhone 拦截日志中查看"
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIFICATION_ID + (number.hashCode() and 0xFFFF), notification)
    }

    private fun ensureChannel(context: Context, nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "来电拦截",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "陌生来电被拦截时通知"
            }
            nm.createNotificationChannel(channel)
        }
    }
}
