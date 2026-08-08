package com.ai4biz.app.model

/**
 * A single input on a generator form. [key] is the stable identifier used to
 * build the AI prompt and to persist the inputs that produced a document.
 */
data class InputField(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val multiline: Boolean = false,
    val required: Boolean = true
)
