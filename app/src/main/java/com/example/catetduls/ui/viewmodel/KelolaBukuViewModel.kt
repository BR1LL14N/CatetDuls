package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.Book
import com.example.catetduls.data.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class KelolaBukuViewModel @Inject constructor(private val repository: BookRepository) :
        ViewModel() {

    // 1. State for Search
    private val _searchQuery = MutableStateFlow("")

    // 2. Main Data Stream (All Books + Search)
    val filteredBooks =
            _searchQuery
                    .flatMapLatest { query ->
                        repository.getAllBooks().map { books ->
                            if (query.isBlank()) {
                                books
                            } else {
                                books.filter { it.name.contains(query, ignoreCase = true) }
                            }
                        }
                    }
                    .asLiveData()

    // 3. Setter for Search
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 4. Actions
    fun activateBook(bookId: Int) {
        viewModelScope.launch { repository.switchActiveBook(bookId) }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch { repository.delete(book) }
    }
}
