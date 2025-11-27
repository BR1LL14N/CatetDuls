package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository untuk operasi Buku
 */
class BookRepository(private val bookDao: BookDao) {

    // READ
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    fun getBookById(bookId: Int): Flow<Book?> = bookDao.getBookById(bookId)

    suspend fun getBookByIdSync(bookId: Int): Book? = bookDao.getBookByIdSync(bookId)

    fun getActiveBook(): Flow<Book?> = bookDao.getActiveBook()

    suspend fun getActiveBookSync(): Book? = bookDao.getActiveBookSync()

    suspend fun getBookCount(): Int = bookDao.getBookCount()

    // CREATE
    suspend fun insert(book: Book): Long {
        if (!book.isValid()) {
            throw IllegalArgumentException("Nama buku tidak boleh kosong")
        }
        return bookDao.insert(book)
    }

    suspend fun insertAll(books: List<Book>) {
        books.forEach { book ->
            if (!book.isValid()) {
                throw IllegalArgumentException("Nama buku tidak boleh kosong")
            }
        }
        bookDao.insertAll(books)
    }

    // UPDATE
    suspend fun update(book: Book) {
        if (!book.isValid()) {
            throw IllegalArgumentException("Nama buku tidak boleh kosong")
        }
        bookDao.update(book.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun switchActiveBook(newActiveBookId: Int) {
        bookDao.switchActiveBook(newActiveBookId)
    }

    // DELETE
    suspend fun delete(book: Book) {
        bookDao.delete(book)
    }

    suspend fun deleteById(bookId: Int) {
        bookDao.deleteById(bookId)
    }

    // HELPER
    suspend fun createDefaultBook(): Long {
        val defaultBook = Book(
            name = "Buku Baru",
            description = "Buku keuangan baru",
            icon = "📖",
            isActive = false
        )
        return insert(defaultBook)
    }
}