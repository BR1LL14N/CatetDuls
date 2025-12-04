package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response

class ApiTestViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _apiResult = MutableStateFlow("Menunggu pengujian...")
    val apiResult: StateFlow<String> = _apiResult

    fun runDynamicApiTest(endpoint: String) {
        _apiResult.value = "Memanggil endpoint: $endpoint"

        viewModelScope.launch {
            try {
                // PENTING: kirim full URL lengkap, BASE_URL + endpoint
                val url = "http://10.0.2.2:8000/api/$endpoint"
                val response = apiService.dynamicGet(url)

                if (response.isSuccessful) {
                    val body = response.body()?.string() ?: "<empty>"
                    _apiResult.value =
                        "✔ SUCCESS (${response.code()})\nEndpoint: $endpoint\n\n$body"
                } else {
                    _apiResult.value =
                        "❌ FAILED (${response.code()})\nEndpoint: $endpoint\n\n${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _apiResult.value =
                    "⛔ KONEKSI GAGAL\nEndpoint: $endpoint\n\nPesan: ${e.message}"
            }
        }
    }


    // ============================
    // FACTORY DISATUKAN DI SINI
    // ============================
    class Factory(private val apiService: ApiService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ApiTestViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ApiTestViewModel(apiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
