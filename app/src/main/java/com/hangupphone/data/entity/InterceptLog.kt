/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: InterceptLog.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 拦截日志的实体类
 */
package com.hangupphone.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intercept_log")
data class InterceptLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long,
    /** InterceptReason.name */
    val reason: String,
    /** 异步 AI 分析结果，如 "外卖/快递"、"营销骚扰" */
    val aiLabel: String? = null,
    val aiConfidence: Float? = null,
    val restored: Boolean = false
)
