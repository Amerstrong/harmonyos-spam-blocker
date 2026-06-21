/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: FallbackCallEnder.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 部分机型或异常情况下的备用电话挂断方案逻辑
 */
package com.hangupphone.service

import android.os.Build
import android.telecom.TelecomManager
import androidx.annotation.RequiresApi

/**
 * 备选挂断方案：仅当应用为「默认拨号应用」时，TelecomManager.endCall() 才可能生效。
 *
 * 优先级低于 [MyCallScreeningService]；华为 HarmonyOS 上成功率因 ROM 而异。
 * 推荐始终使用 CallScreeningService + ROLE_CALL_SCREENING。
 */
object FallbackCallEnder {

    @RequiresApi(Build.VERSION_CODES.P)
    fun endActiveCall(context: android.content.Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telecom.endCall()
        } else {
            false
        }
    }
}
