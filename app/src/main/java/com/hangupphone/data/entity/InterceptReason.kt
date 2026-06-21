/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: InterceptReason.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 定义电话被拦截的具体原因枚举
 */
package com.hangupphone.data.entity

enum class InterceptReason(val displayName: String) {
    BLACKLIST("黑名单"),
    NOT_IN_CONTACTS("不在通讯录"),
    AI_SUSPECT("AI 标记可疑");

    companion object {
        fun fromStored(value: String): InterceptReason =
            entries.find { it.name == value } ?: NOT_IN_CONTACTS
    }
}
