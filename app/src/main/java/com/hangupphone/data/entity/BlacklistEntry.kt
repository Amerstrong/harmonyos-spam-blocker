/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: BlacklistEntry.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 黑名单数据表的实体类
 */
package com.hangupphone.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "black_list")
data class BlacklistEntry(
    @PrimaryKey val phoneNumber: String,
    /** manual | system | imported */
    val source: String,
    val addedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
