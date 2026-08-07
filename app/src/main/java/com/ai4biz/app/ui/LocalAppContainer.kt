package com.ai4biz.app.ui

import androidx.compose.runtime.compositionLocalOf
import com.ai4biz.app.AppContainer

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided -- wrap content in CompositionLocalProvider from MainActivity")
}
