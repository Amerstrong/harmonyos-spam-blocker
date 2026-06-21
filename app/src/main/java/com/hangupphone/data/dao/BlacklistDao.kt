/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: BlacklistDao.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 黑名单数据表的 Data Access Object
 */
package com.hangupphone.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hangupphone.data.entity.BlacklistEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM black_list ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<BlacklistEntry>>

    @Query("SELECT EXISTS(SELECT 1 FROM black_list WHERE phoneNumber = :normalizedNumber LIMIT 1)")
    suspend fun isBlocked(normalizedNumber: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BlacklistEntry)

    @Query("DELETE FROM black_list WHERE phoneNumber = :normalizedNumber")
    suspend fun delete(normalizedNumber: String)

    @Query("SELECT phoneNumber FROM black_list")
    suspend fun getAllNumbers(): List<String>
}
