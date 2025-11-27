package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

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

    fun getCategoriesByBookIdAndType(bookId: Int, type: TransactionType): Flow<List<Category>> {
        return categoryDao.getCategoriesByBookIdAndType(bookId, type)
    }

    fun getAllCategoriesByBook(bookId: Int): Flow<List<Category>> =
        categoryDao.getAllCategoriesByBook(bookId)

    fun searchCategories(bookId: Int, query: String, type: TransactionType?): Flow<List<Category>> {
        val searchQuery = "%$query%"
        return categoryDao.searchCategories(bookId, searchQuery, type)
    }

    suspend fun getAllCategoriesSync(): List<Category> {
        return categoryDao.getAllCategoriesSync()
    }

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertAll(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }
}