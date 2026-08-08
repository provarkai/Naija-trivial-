package com.ai4biz.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai4biz.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    fun signInAsGuest(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signInAsGuest()
            onDone()
        }
    }

    fun signInWithEmail(email: String, onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signInWithEmail(email)
            onDone()
        }
    }
}
