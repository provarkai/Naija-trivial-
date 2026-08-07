package com.ai4biz.app.model

/**
 * A saved result from an AI generator tool. This is the domain-layer shape;
 * [com.ai4biz.app.data.local.GeneratedDocumentEntity] is its Room-persisted
 * counterpart and [com.ai4biz.app.data.repository.DocumentRepository] maps
 * between the two.
 */
data class GeneratedDocument(
    val id: String,
    val toolId: String,
    val toolTitle: String,
    val title: String,
    val content: String,
    val createdAt: Long
)
