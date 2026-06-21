/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: CallBlockRepository.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 拦截逻辑与数据存储的数据仓库，执行来电识别与拦截决策
 */
package com.hangupphone.data.repository

import android.content.Context
import com.hangupphone.ai.CallClassifier
import com.hangupphone.ai.NoOpCallClassifier
import com.hangupphone.data.AppDatabase
import com.hangupphone.data.entity.BlacklistEntry
import com.hangupphone.data.entity.InterceptLog
import com.hangupphone.data.entity.InterceptReason
import com.hangupphone.util.ContactHelper
import com.hangupphone.util.PhoneNumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CallBlockRepository(
    context: Context,
    private val classifier: CallClassifier = NoOpCallClassifier()
) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val blacklistDao = db.blacklistDao()
    private val logDao = db.interceptLogDao()
    private val contactHelper = ContactHelper.getInstance(appContext)

    private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun observeLogs(): Flow<List<InterceptLog>> = logDao.observeAll()
    fun observeBlacklist(): Flow<List<BlacklistEntry>> = blacklistDao.observeAll()

    /**
     * 来电筛查主逻辑，供 CallScreeningService 同步调用。
     * 顺序：黑名单 > 通讯录白名单。
     */
    suspend fun evaluateIncomingCall(rawNumber: String): InterceptDecision {
        val normalized = PhoneNumberUtils.normalize(rawNumber)
        if (normalized.isEmpty()) {
            return InterceptDecision.allow("empty_number")
        }

        if (blacklistDao.isBlocked(normalized)) {
            return InterceptDecision.block(InterceptReason.BLACKLIST, normalized)
        }

        if (!contactHelper.isInContacts(normalized)) {
            return InterceptDecision.block(InterceptReason.NOT_IN_CONTACTS, normalized)
        }

        return InterceptDecision.allow("in_contacts")
    }

    suspend fun recordIntercept(rawNumber: String, reason: InterceptReason): Long {
        val log = InterceptLog(
            phoneNumber = PhoneNumberUtils.normalize(rawNumber),
            timestamp = System.currentTimeMillis(),
            reason = reason.name
        )
        val id = logDao.insert(log)
        scheduleAsyncAiAnalysis(id, rawNumber, reason)
        return id
    }

    suspend fun addToBlacklist(rawNumber: String, source: String = "manual", note: String? = null) {
        blacklistDao.insert(
            BlacklistEntry(
                phoneNumber = PhoneNumberUtils.normalize(rawNumber),
                source = source,
                note = note
            )
        )
    }

    suspend fun removeFromBlacklist(rawNumber: String) {
        blacklistDao.delete(PhoneNumberUtils.normalize(rawNumber))
    }

    /** 误杀恢复：移出黑名单并标记日志 */
    suspend fun restoreFalsePositive(log: InterceptLog) {
        removeFromBlacklist(log.phoneNumber)
        logDao.markRestored(log.id)
    }

    suspend fun importSystemBlockList(numbers: List<String>) {
        numbers.forEach { number ->
            addToBlacklist(number, source = "system")
        }
    }

    fun refreshContacts() {
        contactHelper.refreshSync()
    }

    /** 模拟来电判定，用于无真实骚扰电话时的本地测试 */
    suspend fun simulateIncomingCall(rawNumber: String): SimulationResult {
        val normalized = PhoneNumberUtils.normalize(rawNumber)
        val inBlacklist = normalized.isNotEmpty() && blacklistDao.isBlocked(normalized)
        val inContacts = contactHelper.isInContacts(rawNumber)
        val decision = evaluateIncomingCall(rawNumber)
        return SimulationResult(
            rawNumber = rawNumber,
            normalizedNumber = normalized,
            inContacts = inContacts,
            inBlacklist = inBlacklist,
            decision = decision
        )
    }

    data class SimulationResult(
        val rawNumber: String,
        val normalizedNumber: String,
        val inContacts: Boolean,
        val inBlacklist: Boolean,
        val decision: InterceptDecision
    ) {
        fun toDisplayText(): String = buildString {
            appendLine("原始号码：$rawNumber")
            appendLine("归一化：$normalizedNumber")
            appendLine("在通讯录：${if (inContacts) "是 ✓" else "否 ✗"}")
            appendLine("在黑名单：${if (inBlacklist) "是 ✓" else "否 ✗"}")
            appendLine("─────────────")
            appendLine("判定：${if (decision.shouldBlock) "【拦截】" else "【放行】"}")
            if (decision.shouldBlock) {
                append("原因：${decision.reason?.displayName ?: "未知"}")
            } else {
                append("原因：${decision.detail}")
            }
        }
    }

    private fun scheduleAsyncAiAnalysis(logId: Long, rawNumber: String, reason: InterceptReason) {
        aiScope.launch {
            val result = classifier.classifyCall(
                phoneNumber = rawNumber,
                interceptReason = reason,
                extraContext = emptyMap()
            )
            if (result != null) {
                logDao.updateAiResult(logId, result.label, result.confidence)
            }
        }
    }

    data class InterceptDecision(
        val shouldBlock: Boolean,
        val reason: InterceptReason?,
        val normalizedNumber: String,
        val detail: String
    ) {
        companion object {
            fun allow(detail: String) = InterceptDecision(false, null, "", detail)
            fun block(reason: InterceptReason, number: String) =
                InterceptDecision(true, reason, number, reason.displayName)
        }
    }
}
