/**
 * Copyright (c) 2026 Amerstrong.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * @FileName: CallClassifier.kt
 * @Author: AI开物/Amerstrong
 * @Date: 2026/06/21
 * @Description: 轻量级本地分类器接口与 NoOp/ONNX 占位实现，异步为已拦截电话进行分类打标
 */
package com.hangupphone.ai

import com.hangupphone.data.entity.InterceptReason

/**
 * 轻量化本地模型接入点。
 *
 * 设计原则：不参与实时挂断决策，仅在挂断后异步打标签。
 * 实现方可替换为 ONNX Runtime / TensorFlow Lite 推理。
 */
interface CallClassifier {

    suspend fun classifyCall(
        phoneNumber: String,
        interceptReason: InterceptReason,
        extraContext: Map<String, String>
    ): ClassificationResult?
}

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

/**
 * 默认空实现。接入模型时新建 LocalOnnxCallClassifier 并注入 Repository。
 */
class NoOpCallClassifier : CallClassifier {
    override suspend fun classifyCall(
        phoneNumber: String,
        interceptReason: InterceptReason,
        extraContext: Map<String, String>
    ): ClassificationResult? = null
}

/**
 * 预留：接入 0.5B~1B 本地模型时的骨架实现。
 *
 * ```kotlin
 * class LocalOnnxCallClassifier(context: Context) : CallClassifier {
 *     // private val ortSession = OrtEnvironment.getEnvironment().createSession(...)
 *     override suspend fun classifyCall(...): ClassificationResult? {
 *         // val input = buildFeatureVector(phoneNumber, extraContext)
 *         // val output = ortSession.run(input)
 *         // return ClassificationResult(label = "营销骚扰", confidence = 0.87f)
 *     }
 * }
 * ```
 */
class LocalOnnxCallClassifierStub : CallClassifier {
    override suspend fun classifyCall(
        phoneNumber: String,
        interceptReason: InterceptReason,
        extraContext: Map<String, String>
    ): ClassificationResult? {
        // TODO: 加载 assets/models/call_classifier.onnx 并推理
        return null
    }
}
