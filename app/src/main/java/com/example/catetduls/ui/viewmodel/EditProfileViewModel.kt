package com.example.catetduls.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.User
import com.example.catetduls.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditProfileViewModel
@Inject
constructor(
        private val userRepository: UserRepository,
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
        viewModelScope.launch { _currentUser.value = userRepository.getCurrentUser() }
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

    // Update Foto (Offline-First)
    fun updatePhoto(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Copy image to Internal Storage (Private & Persistent)
                val newPath = copyImageToInternalStorage(uri, applicationContext)

                if (newPath != null) {
                    val currentUser = _currentUser.value
                    if (currentUser != null) {
                        // 2. Update Local Database first (Optimistic UI)
                        // Mark as unsynced so SyncWorker picks it up
                        val updatedUser =
                                currentUser.copy(
                                        photo_url = newPath, // Local path
                                        updated_at = System.currentTimeMillis().toString(),
                                        is_synced = false
                                )
                        userRepository.saveUser(updatedUser)

                        // Update UI immediately
                        _currentUser.value = updatedUser

                        // 3. Trigger One-Time Sync immediately (Background)
                        // We do this via Repository or WorkManager (optional, but good for UX)
                        // For now, we rely on the standard SyncWorker or we can trigger it manually
                        // if needed.
                        // Ideally, UI observes 'currentUser' so it updates automatically.

                        _updateResult.value = Result.success(updatedUser)
                    } else {
                        _updateResult.value = Result.failure(Exception("User belum login"))
                    }
                } else {
                    _updateResult.value =
                            Result.failure(
                                    Exception("Gagal menyimpan file gambar ke internal storage")
                            )
                }
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper: Salin Uri -> File Internal Storage (Persistent)
    private suspend fun copyImageToInternalStorage(uri: Uri, context: Context): String? =
            withContext(Dispatchers.IO) {
                try {
                    // Create directory if not exists
                    val directory = File(context.filesDir, "profile_images")
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }

                    // Generate unique filename
                    val fileName = "IMG_PROFILE_${System.currentTimeMillis()}.jpg"
                    val file = File(directory, fileName)

                    val contentResolver = context.contentResolver
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(file)

                    inputStream?.use { input ->
                        outputStream.use { output -> input.copyTo(output) }
                    }

                    // Return absolute path
                    file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

    fun resetState() {
        _updateResult.value = null
    }
}
