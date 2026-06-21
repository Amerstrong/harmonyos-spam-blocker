/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: InterceptLogAdapter.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 在 UI 列表中展示拦截日志的 RecyclerView 适配器
 */
package com.hangupphone.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hangupphone.data.entity.InterceptLog
import com.hangupphone.data.entity.InterceptReason
import com.hangupphone.databinding.ItemInterceptLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InterceptLogAdapter(
    private val onRestore: (InterceptLog) -> Unit
) : ListAdapter<InterceptLog, InterceptLogAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInterceptLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemInterceptLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: InterceptLog) {
            val reason = InterceptReason.fromStored(log.reason)
            binding.textNumber.text = log.phoneNumber
            binding.textTime.text = dateFormat.format(Date(log.timestamp))
            binding.textReason.text = reason.displayName
            binding.textAiLabel.text = log.aiLabel?.let { "AI: $it (${log.aiConfidence})" } ?: ""
            binding.btnRestore.isEnabled = !log.restored
            binding.btnRestore.text = if (log.restored) "已恢复" else "误杀恢复"
            binding.btnRestore.setOnClickListener { onRestore(log) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<InterceptLog>() {
        override fun areItemsTheSame(old: InterceptLog, new: InterceptLog) = old.id == new.id
        override fun areContentsTheSame(old: InterceptLog, new: InterceptLog) = old == new
    }
}
