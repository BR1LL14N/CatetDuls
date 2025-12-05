package com.example.catetduls.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.catetduls.data.remote.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response

class ApiTestViewModel(
    private val apiService: ApiService
) : ViewModel() {

    private val _apiResult = MutableStateFlow("Menunggu pengujian...")
    val apiResult: StateFlow<String> = _apiResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Test API dengan custom headers
     * @param method GET, POST, PUT, DELETE
     * @param endpoint Relative path
     * @param jsonBody JSON string untuk POST/PUT
     * @param customToken Custom auth token (jika kosong, pakai dari NetworkModule)
     * @param additionalHeaders Additional headers
     */
    fun testApi(
        method: String,
        endpoint: String,
        jsonBody: String? = null,
        customToken: String? = null,
        additionalHeaders: Map<String, String> = emptyMap()
    ) {
        _apiResult.value = "🔄 Memanggil $method $endpoint..."
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Build headers
                val headers = buildMap {
                    putAll(additionalHeaders)

                    // Jika ada custom token, override default
                    if (!customToken.isNullOrBlank()) {
                        put("Authorization", "Bearer $customToken")
                    }

                    // Ensure these headers exist
                    if (!containsKey("Accept")) {
                        put("Accept", "application/json")
                    }
                    if (!containsKey("Content-Type")) {
                        put("Content-Type", "application/json")
                    }
                }

                val response = when (method.uppercase()) {
                    "GET" -> apiService.dynamicGet(endpoint, headers)

                    "POST" -> {
                        val body = (jsonBody ?: "{}").toRequestBody("application/json".toMediaType())
                        apiService.dynamicPost(endpoint, body, headers)
                    }

                    "PUT" -> {
                        val body = (jsonBody ?: "{}").toRequestBody("application/json".toMediaType())
                        apiService.dynamicPut(endpoint, body, headers)
                    }

                    "DELETE" -> apiService.dynamicDelete(endpoint, headers)

                    else -> {
                        _apiResult.value = "❌ Method tidak valid: $method\nGunakan: GET, POST, PUT, atau DELETE"
                        _isLoading.value = false
                        return@launch
                    }
                }

                handleResponse(response, method, endpoint, jsonBody, headers)

            } catch (e: Exception) {
                handleError(e, method, endpoint)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleResponse(
        response: Response<ResponseBody>,
        method: String,
        endpoint: String,
        requestBody: String?,
        requestHeaders: Map<String, String>
    ) {
        val responseBody = response.body()?.string() ?: ""
        val errorBody = response.errorBody()?.string() ?: ""

        _apiResult.value = buildString {
            appendLine("═══════════════════════════════════")
            appendLine("📡 API TEST RESULT")
            appendLine("═══════════════════════════════════")
            appendLine()



            // Response Status
            when (response.code()) {
                in 200..299 -> appendLine("✅ SUCCESS (${response.code()})")
                401 -> {
                    appendLine("🔒 UNAUTHORIZED (401)")
                    appendLine()
                    appendLine("💡 Tips:")
                    appendLine("   - Token tidak valid atau expired")
                    appendLine("   - Periksa format: Bearer <token>")
                    appendLine("   - Token mungkin tidak dikirim")
                }
                404 -> {
                    appendLine("❌ NOT FOUND (404)")
                    appendLine()
                    appendLine("💡 Tips:")
                    appendLine("   - Route tidak terdaftar di Laravel")
                    appendLine("   - Periksa ejaan endpoint")
                }
                422 -> {
                    appendLine("⚠️ VALIDATION ERROR (422)")
                    appendLine()
                    appendLine("💡 Tips:")
                    appendLine("   - Data tidak valid")
                    appendLine("   - Periksa format JSON")
                }
                500 -> {
                    appendLine("⛔ SERVER ERROR (500)")
                    appendLine()
                    appendLine("💡 Tips:")
                    appendLine("   - Error di Laravel")
                    appendLine("   - Check: storage/logs/laravel.log")
                }
                else -> appendLine("❌ ERROR (${response.code()}): ${response.message()}")
            }

            appendLine()
            appendLine("═══════════════════════════════════")
            appendLine()



            // Response Headers
            appendLine("📥 Response Headers:")
            response.headers().forEach { (name, value) ->
                appendLine("   $name: $value")
            }

            appendLine()
            appendLine("═══════════════════════════════════")
            appendLine()

            // Response Body
            if (response.isSuccessful && responseBody.isNotBlank()) {
                appendLine("📦 Response Body:")
                appendLine(formatJson(responseBody))
            } else if (errorBody.isNotBlank()) {
                appendLine("⚠️ Error Body:")
                appendLine(formatJson(errorBody))
            } else {
                appendLine("📦 Response Body: <empty>")
            }

            appendLine()
            appendLine("═══════════════════════════════════")

            // Request Info
            appendLine("📤 REQUEST:")
            appendLine("   Method: $method")
            appendLine("   Endpoint: $endpoint")
            appendLine()

            // Request Headers
            appendLine("📋 Request Headers:")
            requestHeaders.forEach { (key, value) ->
                if (key.equals("Authorization", ignoreCase = true)) {
                    // Mask token untuk keamanan
                    val maskedValue = if (value.startsWith("Bearer ")) {
                        "Bearer ${value.substring(7, minOf(15, value.length))}..."
                    } else value
                    appendLine("   $key: $maskedValue")
                } else {
                    appendLine("   $key: $value")
                }
            }

            if (!requestBody.isNullOrBlank()) {
                appendLine()
                appendLine("📝 Request Body:")
                appendLine(formatJson(requestBody))
            }

            appendLine()
            appendLine("═══════════════════════════════════")
            appendLine()
        }
    }

    private fun handleError(e: Exception, method: String, endpoint: String) {
        _apiResult.value = buildString {
            appendLine("═══════════════════════════════════")
            appendLine("⛔ CONNECTION ERROR")
            appendLine("═══════════════════════════════════")
            appendLine()
            appendLine("📤 REQUEST:")
            appendLine("   Method: $method")
            appendLine("   Endpoint: $endpoint")
            appendLine()
            appendLine("❌ Error: ${e.javaClass.simpleName}")
            appendLine("❌ Message: ${e.message}")
            appendLine()
            appendLine("💡 Kemungkinan:")
            appendLine("   - Server tidak running")
            appendLine("   - Koneksi timeout")
            appendLine("   - URL salah")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(e.stackTraceToString())
        }
    }

    private fun formatJson(json: String): String {
        return try {
            if (json.isBlank()) return "<empty>"
            // Simple pretty print
            json.replace(",", ",\n  ")
                .replace("{", "{\n  ")
                .replace("}", "\n}")
                .replace("[", "[\n  ")
                .replace("]", "\n]")
        } catch (e: Exception) {
            json
        }
    }

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