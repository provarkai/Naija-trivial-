package com.ai4biz.app.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Unwraps a Compose [Context] (often a ContextWrapper) down to its Activity, if any. */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
