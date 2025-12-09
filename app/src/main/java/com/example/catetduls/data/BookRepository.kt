package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow
import java.lang.IllegalArgumentException

/**
 * Repository untuk operasi Buku
 * Disesuaikan untuk mendukung mekanisme sinkronisasi offline-first.
 */
class BookRepository(private val bookDao: BookDao) {

    // ===================================
    // READ
    // ===================================

    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks()

    fun getBookById(bookId: Int): Flow<Book?> = bookDao.getBookById(bookId)

    suspend fun getBookByIdSync(bookId: Int): Book? = bookDao.getBookByIdSync(bookId)

    fun getActiveBook(): Flow<Book?> = bookDao.getActiveBook()

    suspend fun getActiveBookSync(): Book? = bookDao.getActiveBookSync()

    suspend fun getBookCount(): Int = bookDao.getBookCount()

    // ===================================
    // CREATE
    // ===================================

    suspend fun insert(book: Book): Long {
        if (!book.isValid()) {
            throw IllegalArgumentException("Nama buku tidak boleh kosong")
        }


        val bookToInsert = book.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "CREATE",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )


        return bookDao.insert(bookToInsert)
    }

    suspend fun insertAll(books: List<Book>) {
        val booksToInsert = books.map { book ->
            if (!book.isValid()) {
                throw IllegalArgumentException("Nama buku tidak boleh kosong")
            }
            book.copy(
                isSynced = false,
                isDeleted = false,
                syncAction = "CREATE",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        bookDao.insertAll(booksToInsert)
    }

    // ===================================
    // UPDATE
    // ===================================

    suspend fun update(book: Book) {
        if (!book.isValid()) {
            throw IllegalArgumentException("Nama buku tidak boleh kosong")
        }


        val bookToUpdate = book.copy(
            isSynced = false,
            isDeleted = false,
            syncAction = "UPDATE",
            updatedAt = System.currentTimeMillis()
        )


        bookDao.update(bookToUpdate)
    }

    suspend fun switchActiveBook(newActiveBookId: Int) {
        // Logika switch aktif tidak memerlukan flag sync, karena ini adalah state lokal
        // kecuali Anda ingin menyinkronkan status buku aktif ke server (misalnya untuk preferensi user)
        bookDao.switchActiveBook(newActiveBookId)
    }

    // ===================================
    // DELETE
    // ===================================

    /**
     * Menandai buku sebagai terhapus (soft delete) untuk disinkronkan ke server.
     * Jika buku belum pernah disinkronkan (server_id null), maka hapus permanen lokal.
     */
    suspend fun delete(book: Book) {
        if (book.serverId == null) {
            bookDao.delete(book)
        } else {

            val bookToDelete = book.copy(
                isSynced = false,
                isDeleted = true,
                syncAction = "DELETE",
                updatedAt = System.currentTimeMillis()
            )
            bookDao.update(bookToDelete)
        }
    }

    /**
     * Menghapus secara permanen dari lokal (biasanya hanya dipanggil setelah sync DELETE berhasil)
     */
    suspend fun deleteByIdPermanently(bookId: Int) {
        bookDao.deleteById(bookId)
    }

    // ===================================
    // HELPER
    // ===================================

    suspend fun createDefaultBook(): Long {
        val defaultBook = Book(
            name = "Buku Baru",
            description = "Buku keuangan baru",
            icon = "📖",
            isActive = false,
            lastSyncAt = 0L
        )

        return insert(defaultBook)
    }

    // ===================================
    // SYNC METHODS (Dipanggil oleh Sync Worker)
    // ===================================

    /**
     * Mengambil semua buku yang belum tersinkronisasi atau ditandai DELETE
     */
    suspend fun getUnsyncedBooks(): List<Book> {
        return bookDao.getUnsyncedBooks()
    }

    /**
     * Memperbarui status sinkronisasi setelah operasi server berhasil (CREATE/UPDATE/DELETE).
     */
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long) {
        bookDao.updateSyncStatus(localId, serverId, lastSyncAt)
    }

    /**
     * Menyimpan data buku yang diterima dari server (untuk operasi PULL/READ dari server)
     */
    suspend fun saveFromRemote(book: Book) {
        bookDao.insert(book.copy(
            isSynced = true,
            isDeleted = false,
            syncAction = null,
            lastSyncAt = System.currentTimeMillis()
        ))
    }
}