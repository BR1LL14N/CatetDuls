package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.Book
import com.example.catetduls.data.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FormBukuViewModel @Inject constructor(private val repository: BookRepository) : ViewModel() {

    sealed class UiEvent {
        object Success : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun saveBook(id: Int, name: String, description: String, icon: String, existingBook: Book?) {
        if (name.isBlank()) {
            sendEvent(UiEvent.Error("Nama buku tidak boleh kosong"))
            return
        }

        viewModelScope.launch {
            try {
                if (existingBook == null) {
                    // Create New
                    val newBook =
                            Book(
                                    name = name,
                                    description = description,
                                    icon = icon,
                                    isActive = false,
                                    lastSyncAt = 0L
                            )
                    repository.insert(newBook)
                } else {
                    // Update Existing
                    val updatedBook =
                            existingBook.copy(name = name, description = description, icon = icon)
                    repository.update(updatedBook)
                }
                sendEvent(UiEvent.Success)
            } catch (e: Exception) {
                sendEvent(UiEvent.Error(e.message ?: "Terjadi kesalahan"))
            }
        }
    }

    private fun sendEvent(event: UiEvent) {
        viewModelScope.launch { _uiEvent.send(event) }
    }
}
