// file: KelolaKategoriViewModel.kt
package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.example.catetduls.data.CategoryRepository
import com.example.catetduls.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class KelolaKategoriViewModel(
    private val repository: CategoryRepository,
    private val activeBookId: Int // <-- TAMBAH: Untuk filtering Multi-Book
) : ViewModel() {

    // 1. State untuk Filter dan Search
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<TransactionType?>(null) // null = Semua

    // 2. Query Utama
    val filteredCategories = _searchQuery.combine(_selectedType) { query, type ->
        Pair(query, type)
    }.flatMapLatest { (query, type) ->
        if (query.isBlank() && type == null) {
            // Jika tidak ada filter/search, ambil semua kategori buku aktif
            repository.getAllCategoriesByBook(activeBookId)
        } else {
            // Lakukan query filtering dengan kombinasi
            repository.searchCategories(activeBookId, query, type)
        }
    }.asLiveData()

    // 3. Setter untuk State
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: TransactionType?) {
        _selectedType.value = type
    }
}

// Perbarui Factory
class KelolaKategoriViewModelFactory(
    private val repository: CategoryRepository,
    private val activeBookId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KelolaKategoriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KelolaKategoriViewModel(repository, activeBookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}