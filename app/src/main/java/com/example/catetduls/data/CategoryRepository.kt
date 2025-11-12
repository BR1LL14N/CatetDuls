package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk mengelola data Kategori.
 *
 * (Nanti kita akan menggunakan Hilt untuk 'meng-inject' categoryDao ke sini)
 */
class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}