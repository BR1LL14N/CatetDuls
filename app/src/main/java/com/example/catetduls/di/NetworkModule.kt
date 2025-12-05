package com.example.catetduls.di

import com.example.catetduls.data.remote.ApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {


    private const val AUTH_TOKEN = "1|xzi277UzdQLZjdDmKn6lXEs54mgZphtDEyPf2sAB4d8a87de"


    private const val BASE_URL = "http://10.0.2.2:8000/api/"  // TAMBAH SLASH DI AKHIR!

    private const val TIMEOUT_SECONDS = 30L

    fun provideOkHttpClient(): OkHttpClient {

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)  // PENTING: Ubah jadi BODY
        }


        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $AUTH_TOKEN")
                .addHeader("Accept", "application/json")  // PENTING: Tambahkan ini
                .addHeader("Content-Type", "application/json")  // PENTING: Tambahkan ini
                .build()

            val response = chain.proceed(newRequest)


            println("=== REQUEST ===")
            println("URL: ${originalRequest.url}")
            println("Method: ${originalRequest.method}")
            println("Headers: ${newRequest.headers}")
            println("=== RESPONSE ===")
            println("Code: ${response.code}")
            println("Message: ${response.message}")

            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    val apiService: ApiService by lazy {
        val client = provideOkHttpClient()
        val retrofit = provideRetrofit(client)
        provideApiService(retrofit)
    }
}