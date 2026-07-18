package com.localforge.app.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val id: String,
    val `object`: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String = "stop"
)

@Serializable
data class ModelList(
    val `object`: String = "list",
    val data: List<ModelItem>
)

@Serializable
data class ModelItem(
    val id: String,
    val `object`: String = "model",
    val created: Long = 1677610602,
    val owned_by: String = "localforge"
)
