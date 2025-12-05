package com.example.catetduls.di

import com.example.catetduls.data.remote.ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // Ganti dengan Base URL server backend Anda.
    // Jika masih di tahap testing lokal, gunakan IP address komputer Anda, bukan "localhost".
    // Contoh: "http://192.168.1.5:8080/"
    private const val AUTH_TOKEN = "3|WiiMtSVSRNKUGKeU7yeRueciH2WnjkhjGI01V1UD1842c190"
    private const val BASE_URL = "http://10.0.2.2:8000/api/"
    private const val TIMEOUT_SECONDS = 30L

    // ===========================================
    // 1. Menyediakan OkHttpClient
    // ===========================================

    fun provideOkHttpClient(): OkHttpClient {
        // Interceptor untuk logging (membantu debugging API)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Ubah menjadi HttpLoggingInterceptor.Level.BODY untuk melihat payload dan response
            setLevel(HttpLoggingInterceptor.Level.NONE)
        }

        val authInterceptor = Interceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                .build()
            chain.proceed(req)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    // ===========================================
    // 2. Menyediakan Retrofit Instance
    // ===========================================

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            // Menggunakan Gson Converter. Pastikan dependensi sudah ada.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ===========================================
    // 3. Menyediakan ApiService (Contract Retrofit)
    // ===========================================

    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // ===========================================
    // Fungsi Init (Untuk DI Manual)
    // ===========================================

    /**
     * Fungsi helper untuk menginisialisasi semua instance.
     * Biasanya dipanggil di Application class pertama kali.
     */
    val apiService: ApiService by lazy {
        val client = provideOkHttpClient()
        val retrofit = provideRetrofit(client)
        provideApiService(retrofit)
    }
}
