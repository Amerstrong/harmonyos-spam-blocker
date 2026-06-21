/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: ContactHelper.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 手机通讯录白名单加载与判断的辅助工具类
 */
package com.hangupphone.util

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 系统通讯录白名单缓存。
 * CallScreeningService 回调必须在极短时间内完成，禁止在 onScreenCall 里做全量 ContentResolver 扫描。
 */
class ContactHelper private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()

    @Volatile
    private var cachedNumbers: Set<String> = emptySet()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshSync()
        }
    }

    init {
        if (hasContactsPermission()) {
            refreshSync()
            registerObserver()
        }
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

    private fun registerObserver() {
        try {
            appContext.contentResolver.registerContentObserver(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                true,
                observer
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "无法注册通讯录监听，权限未授予", e)
        }
    }

    fun refreshSync() {
        if (!hasContactsPermission()) {
            cachedNumbers = emptySet()
            return
        }
        if (cachedNumbers.isEmpty()) {
            registerObserver()
        }
        cachedNumbers = loadAllPhoneNumbers()
    }

    suspend fun refresh() {
        mutex.withLock {
            cachedNumbers = loadAllPhoneNumbers()
        }
    }

    fun isInContacts(rawNumber: String): Boolean {
        val target = PhoneNumberUtils.normalize(rawNumber)
        if (target.isEmpty()) return false
        val snapshot = cachedNumbers
        return snapshot.any { PhoneNumberUtils.matches(it, target) }
    }

    private fun loadAllPhoneNumbers(): Set<String> {
        if (!hasContactsPermission()) return emptySet()
        val numbers = mutableSetOf<String>()
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        try {
            appContext.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (index < 0) return@use
                while (cursor.moveToNext()) {
                    val raw = cursor.getString(index)
                    val normalized = PhoneNumberUtils.normalize(raw)
                    if (normalized.isNotEmpty()) {
                        numbers.add(normalized)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "读取通讯录失败，请先在应用内授予权限", e)
        }
        return numbers
    }

    companion object {
        private const val TAG = "ContactHelper"
        @Volatile
        private var instance: ContactHelper? = null

        fun getInstance(context: Context): ContactHelper =
            instance ?: synchronized(this) {
                instance ?: ContactHelper(context).also { instance = it }
            }
    }
}
