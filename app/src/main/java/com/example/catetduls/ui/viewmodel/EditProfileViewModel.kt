package com.example.catetduls.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.User
import com.example.catetduls.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    // 👇 PERBAIKAN: Tambahkan 'private val' dan ganti nama jadi 'applicationContext' agar aman
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _updateResult = MutableStateFlow<Result<User>?>(null)
    val updateResult: StateFlow<Result<User>?> = _updateResult.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUser()
        }
    }

    // Update Profile Text
    fun updateProfile(name: String, email: String) {
        if (name.isBlank() || email.isBlank()) {
            _updateResult.value = Result.failure(Exception("Nama dan Email tidak boleh kosong"))
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _updateResult.value = userRepository.updateProfile(name, email)
            _isLoading.value = false
        }
    }

    // Update Foto
    fun updatePhoto(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Ubah URI Galeri menjadi File Asli di Cache
                // 👇 Gunakan 'applicationContext' yang sudah dideklarasikan di atas
                val file = uriToFile(uri, applicationContext)

                if (file != null) {
                    // 2. Kirim File ke Repository
                    val result = userRepository.uploadPhoto(file)
                    _updateResult.value = result
                } else {
                    _updateResult.value = Result.failure(Exception("Gagal memproses file gambar"))
                }
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper: Salin Uri -> File Cache
    private suspend fun uriToFile(uri: Uri, context: Context): File? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val tempFile = File.createTempFile("upload_img", ".jpg", context.cacheDir)

            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resetState() {
        _updateResult.value = null
    }
}