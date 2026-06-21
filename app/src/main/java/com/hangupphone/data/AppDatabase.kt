/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: AppDatabase.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: Room 数据库类，管理黑名单和拦截日志的数据表
 */
package com.hangupphone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hangupphone.data.dao.BlacklistDao
import com.hangupphone.data.dao.InterceptLogDao
import com.hangupphone.data.entity.BlacklistEntry
import com.hangupphone.data.entity.InterceptLog

@Database(
    entities = [BlacklistEntry::class, InterceptLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun blacklistDao(): BlacklistDao
    abstract fun interceptLogDao(): InterceptLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hangup_phone.db"
                ).build().also { instance = it }
            }
    }
}
