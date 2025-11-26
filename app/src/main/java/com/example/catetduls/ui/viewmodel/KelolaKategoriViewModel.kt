package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.catetduls.data.Category
import com.example.catetduls.data.CategoryRepository

class KelolaKategoriViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val allCategories = repository.getAllCategories().asLiveData()
}

class KelolaKategoriViewModelFactory(
    private val repository: CategoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(KelolaKategoriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return KelolaKategoriViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}