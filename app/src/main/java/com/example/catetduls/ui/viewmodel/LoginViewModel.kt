package com.example.catetduls.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.User
import com.example.catetduls.data.UserRepository
import com.example.catetduls.data.local.TokenManager
import com.example.catetduls.data.remote.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow<User?>(null)
    val loginSuccess: StateFlow<User?> = _loginSuccess.asStateFlow()

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Email dan Password wajib diisi"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = userRepository.login(email, pass)

                result.onSuccess { user ->
                    // ✅ PERBAIKAN: Gunakan access_token (bukan token)
                    user.access_token?.let { token ->
                        TokenManager.saveToken(context, token)
                    }

                    _loginSuccess.value = user
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Login gagal"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearState() {
        _errorMessage.value = null
        _loginSuccess.value = null
    }
}