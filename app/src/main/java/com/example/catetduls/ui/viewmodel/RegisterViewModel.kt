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

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _registerSuccess = MutableStateFlow<User?>(null)
    val registerSuccess: StateFlow<User?> = _registerSuccess.asStateFlow()

    /**
     * Register user baru
     */
    fun register(name: String, email: String, password: String, passwordConfirmation: String) {
        // Validasi input
        if (name.isBlank()) {
            _errorMessage.value = "Nama wajib diisi"
            return
        }

        if (email.isBlank()) {
            _errorMessage.value = "Email wajib diisi"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Format email tidak valid"
            return
        }

        if (password.isBlank()) {
            _errorMessage.value = "Password wajib diisi"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Password minimal 6 karakter"
            return
        }

        if (password != passwordConfirmation) {
            _errorMessage.value = "Konfirmasi password tidak sesuai"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = userRepository.register(name, email, password, passwordConfirmation)

                result.onSuccess { user ->
                    _registerSuccess.value = user
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "Registrasi gagal"
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
        _registerSuccess.value = null
    }
}