/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: SystemBlocklistImporter.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 导入系统自带黑名单数据的辅助工具类
 */
package com.hangupphone.util

import android.content.Context
import com.hangupphone.data.repository.CallBlockRepository

/**
 * 系统骚扰拦截名单导入（华为 / 原生差异较大，无统一公开 API）。
 *
 * 可行路径：
 * 1. 华为「骚扰拦截」若导出为文件，解析后调用 [CallBlockRepository.importSystemBlockList]
 * 2. 部分 ROM 存在非公开 ContentProvider，需 adb 探测后适配（个人本地部署可接受）
 * 3. 用户手动粘贴号码批量导入
 */
object SystemBlocklistImporter {

    suspend fun importFromTextLines(
        repository: CallBlockRepository,
        lines: String
    ): Int {
        val numbers = lines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { PhoneNumberUtils.normalize(it) }
            .filter { it.isNotEmpty() }
            .distinct()
        repository.importSystemBlockList(numbers)
        return numbers.size
    }

    /**
     * 占位：探测华为骚扰拦截 Provider（需按实际机型逆向/adb 确认 URI）。
     */
    @Suppress("UNUSED_PARAMETER")
    fun probeHuaweiBlocklistProvider(context: Context): List<String> {
        // val uri = Uri.parse("content://com.huawei.systemmanager/blocklist")
        // context.contentResolver.query(uri, ...) 
        return emptyList()
    }
}
