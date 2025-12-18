package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.User
import com.example.catetduls.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * LoginViewModel - Clean & Simple
 *
 * Token management sudah dilakukan di UserRepository,
 * jadi ViewModel hanya fokus ke UI state
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _loginSuccess = MutableStateFlow<User?>(null)
    val loginSuccess: StateFlow<User?> = _loginSuccess.asStateFlow()

    private val _forgotPasswordSuccess = MutableStateFlow<String?>(null)
    val forgotPasswordSuccess: StateFlow<String?> = _forgotPasswordSuccess.asStateFlow()

    /**
     * Login user
     */
    fun login(email: String, password: String) {
        // Validasi input
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Email dan Password wajib diisi"
            return
        }

        // Validasi email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Format email tidak valid"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = userRepository.login(email, password)

                result.onSuccess { user ->
                    // ✅ Token sudah tersimpan di UserRepository.login()
                    // Tidak perlu panggil TokenManager lagi!

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

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _forgotPasswordSuccess.value = null

            val result = userRepository.forgotPassword(email)

            _isLoading.value = false

            result.fold(
                onSuccess = { message ->
                    _forgotPasswordSuccess.value = message
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Permintaan reset password gagal."
                }
            )
        }
    }

    fun clearState() {
        _errorMessage.value = null
        _loginSuccess.value = null
        _forgotPasswordSuccess.value = null
    }
}