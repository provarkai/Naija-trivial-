package com.ai4biz.app.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val BUSINESS_SETUP = "business_setup"
    const val HOME = "home"
    const val ASSISTANT = "assistant"
    const val GENERATOR = "generator/{toolId}"
    const val RESULT = "result/{documentId}"
    const val WORKSPACE = "workspace"
    const val SUBSCRIPTION = "subscription"
    const val PROFILE = "profile"

    fun generator(toolId: String) = "generator/$toolId"
    fun result(documentId: String) = "result/$documentId"
}
