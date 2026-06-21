/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: InterceptLogDao.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 拦截日志数据表的 Data Access Object
 */
package com.hangupphone.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hangupphone.data.entity.InterceptLog
import kotlinx.coroutines.flow.Flow

@Dao
interface InterceptLogDao {

    @Query("SELECT * FROM intercept_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<InterceptLog>>

    @Insert
    suspend fun insert(log: InterceptLog): Long

    @Update
    suspend fun update(log: InterceptLog)

    @Query("UPDATE intercept_log SET aiLabel = :label, aiConfidence = :confidence WHERE id = :id")
    suspend fun updateAiResult(id: Long, label: String, confidence: Float)

    @Query("UPDATE intercept_log SET restored = 1 WHERE id = :id")
    suspend fun markRestored(id: Long)
}
