/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: MainActivity.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 主界面，展示拦截日志、拦截状态及手动黑名单管理
 */
package com.hangupphone.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hangupphone.data.repository.CallBlockRepository
import com.hangupphone.databinding.ActivityMainBinding
import com.hangupphone.util.PermissionHelper
import com.hangupphone.util.ScreeningEventStore
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: CallBlockRepository
    private lateinit var logAdapter: InterceptLogAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "部分权限未授予，拦截功能可能失效", Toast.LENGTH_LONG).show()
        }
        refreshSetupStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = CallBlockRepository(this)

        setupRecyclerView()
        setupButtons()
        requestCorePermissions()
        refreshDiagnostic()
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.hasContactsPermission(this)) {
            repository.refreshContacts()
        }
        refreshSetupStatus()
        refreshDiagnostic()
    }

    private fun refreshDiagnostic() {
        binding.textDiagnostic.text = PermissionHelper.getDiagnosticReport(this)
        binding.textScreeningHistory.text = ScreeningEventStore.getEventHistory(this)
    }

    private fun setupRecyclerView() {
        logAdapter = InterceptLogAdapter(
            onRestore = { log ->
                AlertDialog.Builder(this)
                    .setTitle("误杀恢复")
                    .setMessage("将 ${log.phoneNumber} 移出黑名单，并标记为已恢复？\n（该号码已在通讯录中才会正常放行）")
                    .setPositiveButton("确认") { _, _ ->
                        lifecycleScope.launch {
                            repository.restoreFalsePositive(log)
                            Toast.makeText(this@MainActivity, "已恢复", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )
        binding.recyclerLogs.layoutManager = LinearLayoutManager(this)
        binding.recyclerLogs.adapter = logAdapter

        lifecycleScope.launch {
            repository.observeLogs().collect { logs ->
                logAdapter.submitList(logs)
                binding.textEmptyLogs.visibility =
                    if (logs.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }
    }

    private fun setupButtons() {
        binding.btnGrantScreening.setOnClickListener {
            PermissionHelper.requestCallScreeningRole(this, REQ_CALL_SCREENING)
        }
        binding.btnOpenDefaultApps.setOnClickListener {
            PermissionHelper.openDefaultAppsSettings(this)
        }
        binding.btnAddBlacklist.setOnClickListener {
            val input = binding.editBlacklist.text?.toString()?.trim().orEmpty()
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入号码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                repository.addToBlacklist(input)
                binding.editBlacklist.text?.clear()
                Toast.makeText(this@MainActivity, "已加入黑名单", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnRefreshContacts.setOnClickListener {
            if (!PermissionHelper.hasContactsPermission(this)) {
                Toast.makeText(this, "请先在设置向导中授予通讯录权限", Toast.LENGTH_LONG).show()
                startActivity(android.content.Intent(this, SetupActivity::class.java))
                return@setOnClickListener
            }
            repository.refreshContacts()
            Toast.makeText(this, "通讯录缓存已刷新", Toast.LENGTH_SHORT).show()
        }
        binding.btnOpenSetup.setOnClickListener {
            startActivity(android.content.Intent(this, SetupActivity::class.java))
        }
        binding.btnHuaweiBackground.setOnClickListener {
            PermissionHelper.openHuaweiBackgroundSettings(this)
        }
        binding.btnRefreshDiagnostic.setOnClickListener { refreshDiagnostic() }
        binding.btnSimulate.setOnClickListener { runSimulation(writeLog = false) }
        binding.btnSimulateAndLog.setOnClickListener { runSimulation(writeLog = true) }
    }

    private fun runSimulation(writeLog: Boolean) {
        val input = binding.editTestNumber.text?.toString()?.trim().orEmpty()
        if (input.isEmpty()) {
            Toast.makeText(this, "请输入测试号码", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val result = repository.simulateIncomingCall(input)
            binding.textSimulateResult.text = result.toDisplayText()
            if (writeLog && result.decision.shouldBlock) {
                repository.recordIntercept(input, result.decision.reason!!)
                Toast.makeText(this@MainActivity, "已写入拦截日志", Toast.LENGTH_SHORT).show()
            } else if (writeLog) {
                Toast.makeText(this@MainActivity, "该号码会放行，未写入日志", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCorePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun refreshSetupStatus() {
        val checklist = PermissionHelper.getSetupChecklist(this)
        binding.textSetupStatus.text = checklist.joinToString("\n") { item ->
            val mark = if (item.done) "✓" else "✗"
            "$mark ${item.title}"
        }
        val screeningReady = PermissionHelper.isCallScreeningRoleHeld(this)
        binding.btnGrantScreening.isEnabled = !screeningReady
    }

    companion object {
        private const val REQ_CALL_SCREENING = 1001
    }
}
