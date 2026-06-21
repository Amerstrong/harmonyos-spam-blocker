/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: PhoneNumberUtils.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 电话号码格式化、归一化处理工具类
 */
package com.hangupphone.util

/**
 * 统一号码归一化，便于通讯录 / 黑名单 / 来电比对。
 * 华为 HarmonyOS 来电可能带 +86、0086、空格、短横线等格式。
 */
object PhoneNumberUtils {

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("0086") && digits.length > 4 -> digits.substring(4)
            digits.startsWith("86") && digits.length > 11 -> digits.substring(2)
            else -> digits
        }
    }

    /** 末尾 11 位匹配，兼容固话 / 不带区号等场景 */
    fun matches(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        val minLen = 7
        if (na.length >= minLen && nb.length >= minLen) {
            val suffixLen = minOf(na.length, nb.length, 11)
            return na.takeLast(suffixLen) == nb.takeLast(suffixLen)
        }
        return false
    }
}
