/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: PermissionHelper.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 管理权限申请、验证及系统设置跳转的辅助工具类
 */
package com.hangupphone.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.appcompat.app.AppCompatActivity

/**
 * 权限与「默认应用」引导工具。
 *
 * CallScreeningService 生效的硬性前提：
 * 1. READ_CONTACTS + READ_PHONE_STATE 已授予
 * 2. 应用被设为 Call Screening 角色持有者（Android 10+ RoleManager.ROLE_CALL_SCREENING）
 *
 * 备选方案（部分华为机型）：设为默认拨号应用后可配合 TelecomManager.endCall()，
 * 但 CallScreeningService 更稳定，无需反射。
 */
object PermissionHelper {

    fun hasContactsPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun hasPhoneStatePermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    fun isCallScreeningRoleHeld(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun isDefaultDialer(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return context.packageName == telecom.defaultDialerPackage
    }

    /** 请求「来电筛选」角色 — 系统会弹出授权对话框 */
    fun requestCallScreeningRole(activity: AppCompatActivity, requestCode: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = activity.getSystemService(RoleManager::class.java) ?: return
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            openAppSettings(activity)
            return
        }
        if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    /** 备选：请求成为默认拨号应用 */
    fun requestDefaultDialer(activity: AppCompatActivity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val telecom = activity.getSystemService(TelecomManager::class.java) ?: return
            if (activity.packageName != telecom.defaultDialerPackage) {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.packageName)
                }
                activity.startActivityForResult(intent, requestCode)
            }
        }
    }

    /** 华为 HarmonyOS 手动引导：打开默认应用设置页 */
    fun openDefaultAppsSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        }
    }

    fun isCallScreeningRoleAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
    }

    /** 拦截功能是否具备运行条件 */
    fun isInterceptReady(context: Context): Boolean =
        hasContactsPermission(context) &&
            hasPhoneStatePermission(context) &&
            isCallScreeningRoleHeld(context)

    /** 华为：允许后台运行，避免系统杀服务 */
    fun openHuaweiBackgroundSettings(context: Context) {
        val intents = listOf(
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            ),
            Intent().setClassName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            ),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
        }
        openAppSettings(context)
    }

    fun getDiagnosticReport(context: Context): String = buildString {
        appendLine("【权限状态】")
        appendLine("通讯录：${if (hasContactsPermission(context)) "已授予" else "未授予"}")
        appendLine("电话状态：${if (hasPhoneStatePermission(context)) "已授予" else "未授予"}")
        appendLine("来电筛选角色：${if (isCallScreeningRoleHeld(context)) "已持有 ✓" else "未持有 ✗"}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appendLine("系统支持来电筛选：${if (isCallScreeningRoleAvailable(context)) "是" else "否（华为常见）"}")
        }
        appendLine("默认拨号应用：${if (isDefaultDialer(context)) "是" else "否"}")
        appendLine()
        appendLine("【重要说明】")
        appendLine("· 通讯录内的正常来电：不会被拦截，也不会有提示（与以前一样）")
        appendLine("· 仅「不在通讯录」的陌生号码才会被拦截")
        appendLine("· Android10+ 系统对通讯录来电不会回调本应用")
        appendLine()
        append(ScreeningEventStore.getSummary(context))
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getSetupChecklist(context: Context): List<SetupItem> {
        return listOf(
            SetupItem(
                title = "读取通讯录",
                description = "用于白名单：仅允许通讯录内号码来电",
                done = hasContactsPermission(context)
            ),
            SetupItem(
                title = "读取电话状态",
                description = "识别来电号码与通话状态",
                done = hasPhoneStatePermission(context)
            ),
            SetupItem(
                title = "来电筛选服务（必须）",
                description = "设为默认来电筛选应用，CallScreeningService 才能拦截",
                done = isCallScreeningRoleHeld(context)
            ),
            SetupItem(
                title = "默认拨号应用（可选）",
                description = "部分机型需额外设为默认电话应用",
                done = isDefaultDialer(context)
            )
        )
    }

    data class SetupItem(
        val title: String,
        val description: String,
        val done: Boolean
    )
}
