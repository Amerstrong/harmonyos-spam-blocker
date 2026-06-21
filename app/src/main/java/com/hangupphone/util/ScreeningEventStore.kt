/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: ScreeningEventStore.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 记录和暂存前台日志以便实时在界面上渲染的工具类
 */
package com.hangupphone.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记录 CallScreeningService 是否被系统回调，用于排查「拦截不生效」。
 */
object ScreeningEventStore {

    private const val PREFS = "screening_events"
    private const val KEY_EVENTS = "events"
    private const val KEY_LAST_SCREEN = "last_screen_time"
    private const val KEY_LAST_NUMBER = "last_number"
    private const val KEY_LAST_ACTION = "last_action"
    private const val MAX_EVENTS = 30

    fun recordScreening(
        context: Context,
        number: String,
        action: String
    ) {
        val prefs = prefs(context)
        val time = System.currentTimeMillis()
        val line = "${format(time)} | $number | $action"
        val history = prefs.getString(KEY_EVENTS, "").orEmpty()
            .lines()
            .filter { it.isNotBlank() }
            .toMutableList()
        history.add(0, line)
        while (history.size > MAX_EVENTS) history.removeAt(history.lastIndex)
        prefs.edit()
            .putString(KEY_EVENTS, history.joinToString("\n"))
            .putLong(KEY_LAST_SCREEN, time)
            .putString(KEY_LAST_NUMBER, number)
            .putString(KEY_LAST_ACTION, action)
            .apply()
    }

    fun getSummary(context: Context): String {
        val prefs = prefs(context)
        val lastTime = prefs.getLong(KEY_LAST_SCREEN, 0L)
        val lastNumber = prefs.getString(KEY_LAST_NUMBER, "无") ?: "无"
        val lastAction = prefs.getString(KEY_LAST_ACTION, "无") ?: "无"
        return buildString {
            if (lastTime == 0L) {
                appendLine("⚠ 系统从未回调过来电筛选服务")
                append("说明：拦截功能可能未生效，请检查步骤3权限")
            } else {
                appendLine("✓ 上次回调：${format(lastTime)}")
                appendLine("  号码：$lastNumber")
                append("  动作：$lastAction")
            }
        }
    }

    fun getEventHistory(context: Context): String {
        val history = prefs(context).getString(KEY_EVENTS, "").orEmpty()
        return if (history.isBlank()) "（暂无回调记录）" else history
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun format(time: Long): String =
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))
}
