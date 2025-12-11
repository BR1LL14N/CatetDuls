package com.example.catetduls.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorUtils {

    data class ErrorResponse(
        val success: Boolean,
        val message: String?
    )

    fun getReadableMessage(e: Throwable): String {
        return when (e) {
            is UnknownHostException -> "Tidak ada koneksi internet. Periksa sambungan WiFi atau Data Seluler Anda."
            is ConnectException -> "Gagal terhubung ke server. Mohon coba lagi beberapa saat lagi."
            is SocketTimeoutException -> "Waktu koneksi habis (Timeout). Koneksi internet Anda mungkin lambat."
            else -> "Terjadi kesalahan aplikasi: ${e.localizedMessage ?: e.message}"
        }
    }

    fun parseError(response: Response<*>): String {
        val errorBody = response.errorBody()?.string()
        
        if (errorBody.isNullOrEmpty()) {
            return "Terjadi kesalahan pada server (Kode: ${response.code()})"
        }

        return try {
            // Coba parse format JSON standar { success: false, message: "..." }
            val apiResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            apiResponse.message ?: "Terjadi kesalahan pada server (Kode: ${response.code()})"
        } catch (e: Exception) {
            try {
                // Fallback: Coba parse sebagai map jika formatnya berbeda (misal Laravel validation errors)
                // Contoh: { "message": "The given data was invalid.", "errors": { "email": ["Email exists"] } }
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val map: Map<String, Any> = Gson().fromJson(errorBody, type)
                
                val message = map["message"] as? String
                val errors = map["errors"] as? Map<*, *>

                if (errors != null && errors.isNotEmpty()) {
                    // Ambil error pertama dari list validasi
                    val firstError = errors.values.firstOrNull()
                    if (firstError is List<*> && firstError.isNotEmpty()) {
                        return firstError.first().toString()
                    }
                }
                
                message ?: "Terjadi kesalahan pada server (Kode: ${response.code()})"
            } catch (e2: Exception) {
               "Gagal memproses respon error (${response.code()})"
            }
        }
    }
}
