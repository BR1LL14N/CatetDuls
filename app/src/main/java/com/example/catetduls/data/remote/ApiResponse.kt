package com.example.catetduls.data.remote

data class ApiResponse<T>(
    val success: Boolean,
    val data: T
)