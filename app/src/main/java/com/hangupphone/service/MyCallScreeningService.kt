/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: MyCallScreeningService.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 继承自 Android CallScreeningService，自动拦截并挂断骚扰电话的核心服务逻辑
 */
package com.hangupphone.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.hangupphone.data.entity.InterceptReason
import com.hangupphone.data.repository.CallBlockRepository
import com.hangupphone.util.InterceptNotificationHelper
import com.hangupphone.util.ScreeningEventStore
import kotlinx.coroutines.runBlocking

class MyCallScreeningService : CallScreeningService() {

    private val repository by lazy { CallBlockRepository(applicationContext) }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = extractPhoneNumber(callDetails)
        ScreeningEventStore.recordScreening(
            applicationContext,
            rawNumber.ifBlank { "未知号码" },
            "onScreenCall 被系统调用"
        )

        val decision = runBlocking {
            repository.evaluateIncomingCall(rawNumber)
        }

        if (decision.shouldBlock) {
            val reason = decision.reason ?: InterceptReason.BLACKLIST
            runBlocking {
                repository.recordIntercept(rawNumber, reason)
            }
            ScreeningEventStore.recordScreening(
                applicationContext,
                rawNumber,
                "已拦截 · ${reason.displayName}"
            )
            InterceptNotificationHelper.showBlocked(applicationContext, rawNumber, reason)
            respondToCall(callDetails, buildRejectResponse())
        } else {
            ScreeningEventStore.recordScreening(
                applicationContext,
                rawNumber,
                "已放行"
            )
            respondToCall(callDetails, buildAllowResponse())
        }
    }

    private fun extractPhoneNumber(details: Call.Details): String {
        return details.handle?.schemeSpecificPart ?: ""
    }

    private fun buildRejectResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
    }

    private fun buildAllowResponse(): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .build()
    }
}
