package com.example.catetduls.data

import android.content.Context

// Extension functions for getting repositories

fun Context.getBookClosingRepository(): BookClosingRepository {
    val database = AppDatabase.getDatabase(this)
    return BookClosingRepository(database.bookClosingDao())
}

fun Context.getMemoRepository(): MemoRepository {
    val database = AppDatabase.getDatabase(this)
    return MemoRepository(database.memoDao())
}
