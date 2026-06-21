/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: SetupActivity.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 引导界面，指导用户开启权限及设置默认来电筛选应用
 */
package com.hangupphone.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hangupphone.databinding.ActivitySetupBinding
import com.hangupphone.util.ContactHelper
import com.hangupphone.util.PermissionHelper

/**
 * 首次启动权限向导。作为 LAUNCHER 入口，避免未授权时主界面逻辑崩溃。
 * 完成必要步骤后进入 [MainActivity]。
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshUi() }

    private val callScreeningLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshUi() }

    private val defaultDialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshUi() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGrantContacts.setOnClickListener { requestContacts() }
        binding.btnGrantPhoneState.setOnClickListener { requestPhoneState() }
        binding.btnGrantScreening.setOnClickListener { requestCallScreening() }
        binding.btnGrantDialer.setOnClickListener { requestDefaultDialer() }
        binding.btnOpenAppSettings.setOnClickListener {
            PermissionHelper.openAppSettings(this)
        }
        binding.btnOpenDefaultApps.setOnClickListener {
            PermissionHelper.openDefaultAppsSettings(this)
        }
        binding.btnEnterApp.setOnClickListener { enterMain() }
        binding.btnSkip.setOnClickListener { enterMain() }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val contactsOk = PermissionHelper.hasContactsPermission(this)
        val phoneOk = PermissionHelper.hasPhoneStatePermission(this)
        val screeningOk = PermissionHelper.isCallScreeningRoleHeld(this)
        val dialerOk = PermissionHelper.isDefaultDialer(this)

        binding.textStepContacts.text = stepLabel("步骤 1：读取通讯录", contactsOk)
        binding.textStepPhone.text = stepLabel("步骤 2：读取电话状态", phoneOk)
        binding.textStepScreening.text = stepLabel("步骤 3：来电筛选服务（必须）", screeningOk)
        binding.textStepDialer.text = stepLabel("步骤 4：默认拨号（华为可选）", dialerOk)

        binding.btnGrantContacts.isEnabled = !contactsOk
        binding.btnGrantPhoneState.isEnabled = !phoneOk
        binding.btnGrantScreening.isEnabled = !screeningOk
        binding.btnGrantDialer.isEnabled = !dialerOk

        val canIntercept = contactsOk && phoneOk && screeningOk
        val roleAvailable = PermissionHelper.isCallScreeningRoleAvailable(this)
        binding.textHint.text = when {
            canIntercept -> {
                "核心权限已就绪。\n" +
                    "注意：通讯录内的正常来电不会有任何变化；仅陌生号码会被拦截。\n" +
                    "请用另一部手机（不在通讯录的号码）拨打测试。"
            }
            !roleAvailable && !screeningOk -> {
                "⚠ 本机可能不支持「来电筛选」角色（华为常见）。\n" +
                    "请尝试：\n" +
                    "1. 步骤4 设为默认拨号应用\n" +
                    "2. 打开「华为后台运行」允许本应用\n" +
                    "3. 用不在通讯录的号码拨打测试\n" +
                    "若主界面「服务诊断」仍显示从未回调，说明系统未接入拦截服务。"
            }
            else -> {
                "请按顺序完成步骤 1→2→3。\n" +
                    "若系统不弹出来电筛选，点「打开默认应用设置」手动选择 HangupPhone。\n" +
                    "华为路径：设置 → 应用 → 默认应用 → 来电筛选/电话助手"
            }
        }
        binding.btnEnterApp.isEnabled = contactsOk && phoneOk
    }

    private fun stepLabel(title: String, done: Boolean): String {
        return if (done) "✓ $title" else "✗ $title"
    }

    private fun requestContacts() {
        if (PermissionHelper.hasContactsPermission(this)) return
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
    }

    private fun requestPhoneState() {
        if (PermissionHelper.hasPhoneStatePermission(this)) return
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
    }

    private fun requestCallScreening() {
        if (PermissionHelper.isCallScreeningRoleHeld(this)) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_CALL_SCREENING)) {
                callScreeningLauncher.launch(
                    roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_CALL_SCREENING)
                )
                return
            }
        }
        Toast.makeText(this, "系统不支持一键授权，请用下方按钮手动设置", Toast.LENGTH_LONG).show()
        PermissionHelper.openDefaultAppsSettings(this)
    }

    private fun requestDefaultDialer() {
        if (PermissionHelper.isDefaultDialer(this)) return
        val intent = Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
            putExtra(
                android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                packageName
            )
        }
        if (intent.resolveActivity(packageManager) != null) {
            defaultDialerLauncher.launch(intent)
        } else {
            PermissionHelper.openDefaultAppsSettings(this)
        }
    }

    private fun enterMain() {
        if (!PermissionHelper.hasContactsPermission(this) || !PermissionHelper.hasPhoneStatePermission(this)) {
            Toast.makeText(this, "请先完成步骤 1 和 2", Toast.LENGTH_SHORT).show()
            return
        }
        ContactHelper.getInstance(this).refreshSync()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
