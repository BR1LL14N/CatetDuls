package com.example.catetduls.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.AppDatabase
import com.example.catetduls.data.Memo
import com.example.catetduls.data.MemoRepository
import com.example.catetduls.data.TagEntity
import com.example.catetduls.data.TagRepository
import kotlinx.coroutines.launch

class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MemoRepository(database.memoDao())
    private val tagRepository = TagRepository(database.tagDao())

    private val _memos = MutableLiveData<List<Memo>>()
    val memos: LiveData<List<Memo>> = _memos

    private val _tags = MutableLiveData<List<TagEntity>>()
    val tags: LiveData<List<TagEntity>> = _tags

    private val _currentMemo = MutableLiveData<Memo?>()
    val currentMemo: LiveData<Memo?> = _currentMemo

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private var currentBookId: Int = 0
    private var selectedTag: String? = null

    init {
        // Get active book ID
        val sharedPrefs = application.getSharedPreferences("app_settings", 0)
        currentBookId = sharedPrefs.getInt("active_book_id", 1)
        loadMemos()
        loadTags()
    }

    private fun loadMemos() {
        viewModelScope.launch {
            val flow =
                    if (selectedTag != null) {
                        repository.getMemosByTag(currentBookId, selectedTag!!)
                    } else {
                        repository.getAllMemosByBook(currentBookId)
                    }

            flow.collect { list -> _memos.postValue(list) }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.allTags.collect { list -> _tags.postValue(list) }
        }
    }

    fun filterByTag(tag: String?) {
        selectedTag = tag
        loadMemos()
    }

    fun loadMemoById(id: Int) {
        viewModelScope.launch {
            try {
                val memo = repository.getMemoById(id)
                _currentMemo.postValue(memo)
            } catch (e: Exception) {
                _error.postValue("Gagal memuat memo: ${e.message}")
            }
        }
    }

    fun saveMemo(title: String, content: String, tags: String = "") {
        viewModelScope.launch {
            try {
                if (title.isBlank()) {
                    _error.postValue("Judul tidak boleh kosong")
                    return@launch
                }
                if (title.isBlank()) {
                    _error.postValue("Judul tidak boleh kosong")
                    return@launch
                }
                // Content can be empty

                repository.saveMemo(
                        bookId = currentBookId,
                        title = title,
                        content = content,
                        tags = tags
                )

                _success.postValue(true)
                loadMemos()
                // loadTags() -> Handled by Flow
            } catch (e: Exception) {
                _error.postValue("Gagal menyimpan memo: ${e.message}")
            }
        }
    }

    fun updateMemo(id: Int, title: String, content: String, tags: String) {
        viewModelScope.launch {
            try {
                if (title.isBlank()) {
                    _error.postValue("Judul tidak boleh kosong")
                    return@launch
                }
                if (title.isBlank()) {
                    _error.postValue("Judul tidak boleh kosong")
                    return@launch
                }
                // Content can be empty

                repository.updateMemo(id, title, content, tags)

                _success.postValue(true)
                loadMemos()
                // loadTags()
            } catch (e: Exception) {
                _error.postValue("Gagal mengupdate memo: ${e.message}")
            }
        }
    }

    fun deleteMemo(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteMemo(id)
                _success.postValue(true)
                loadMemos()
            } catch (e: Exception) {
                _error.postValue("Gagal menghapus memo: ${e.message}")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = false
    }

    fun clearCurrentMemo() {
        _currentMemo.value = null
    }
}

class MemoViewModelFactory(private val application: Application) :
        androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MemoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MemoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
