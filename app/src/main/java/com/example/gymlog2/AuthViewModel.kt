package com.example.gymlog2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    fun skipLogin() {
        _authState.value = AuthState.Authenticated
    }

    fun signOut() {
        authManager.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun getDisplayName(): String = authManager.getDisplayName()
    fun getPhotoUrl(): String = authManager.getPhotoUrl()
    fun getUserId(): String = authManager.getUserId()

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }
}
