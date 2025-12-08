package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.catetduls.data.Category
import com.example.catetduls.data.CategoryRepository
import com.example.catetduls.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FormKategoriViewModel(
    private val repository: CategoryRepository,
    private val bookId: Int,
    private val existingCategory: Category? = null,
    private val defaultType: TransactionType? = null
) : ViewModel() {

    private val _name = MutableStateFlow(existingCategory?.name ?: "")
    val name: StateFlow<String> = _name

    private val _icon = MutableStateFlow(existingCategory?.icon ?: "⚙️")
    val icon: StateFlow<String> = _icon

    private val initialType = existingCategory?.type
        ?: defaultType
        ?: TransactionType.PENGELUARAN

    private val _type = MutableStateFlow(initialType)
    val type: StateFlow<TransactionType> = _type

    private val _isDefault = existingCategory?.isDefault ?: false

    fun setName(value: String) { _name.value = value }
    fun setIcon(value: String) { _icon.value = value }
    fun setType(value: TransactionType) { _type.value = value } // ← TAMBAHKAN INI

    suspend fun saveCategory(providedName: String? = null, providedIcon: String? = null): Boolean {
        val nameToUse = providedName ?: name.value
        val iconToUse = providedIcon ?: icon.value

        if (nameToUse.isNullOrBlank()) {
            return false
        }

        val category = Category(
            id = existingCategory?.id ?: 0,
            bookId = bookId,
            name = nameToUse.trim(),
            icon = iconToUse,
            type = type.value,
            isDefault = _isDefault,
            lastSyncAt = 0L
        )

        repository.insertCategory(category)
        return true
    }
}

// Perbarui Factory
class FormKategoriViewModelFactory(
    private val repository: CategoryRepository,
    private val existingCategory: Category? = null,
    private val defaultType: TransactionType? = null,
    private val bookId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FormKategoriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")

            return FormKategoriViewModel(repository, bookId, existingCategory, defaultType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}