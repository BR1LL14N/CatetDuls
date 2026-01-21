package com.example.catetduls.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.TagEntity
import com.example.catetduls.data.TagRepository
import kotlinx.coroutines.launch

class ManageTagsViewModel(private val repository: TagRepository) : ViewModel() {

    val allTags: LiveData<List<TagEntity>> = repository.allTags.asLiveData()

    fun insert(name: String, color: String) = viewModelScope.launch {
        if (name.isNotBlank()) {
            val newTag = TagEntity(name = name.trim(), color = color)
            repository.insert(newTag)
        }
    }

    fun update(tag: TagEntity, newName: String, newColor: String) = viewModelScope.launch {
        if (newName.isNotBlank()) {
            val updatedTag = tag.copy(name = newName.trim(), color = newColor, updatedAt = System.currentTimeMillis())
            repository.update(updatedTag)
        }
    }

    fun delete(tag: TagEntity) = viewModelScope.launch {
        repository.delete(tag)
    }
}

class ManageTagsViewModelFactory(private val repository: TagRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageTagsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ManageTagsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
