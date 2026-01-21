package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class BookClosingRepository @Inject constructor(private val bookClosingDao: BookClosingDao) :
        SyncRepository<BookClosing> {

    fun getAllClosingsByBook(bookId: Int): Flow<List<BookClosing>> {
        return bookClosingDao.getAllClosingsByBook(bookId)
    }

    suspend fun getClosingForDate(bookId: Int, timestamp: Long): BookClosing? {
        return bookClosingDao.getClosingForDate(bookId, timestamp)
    }

    suspend fun getClosingById(id: Int): BookClosing? {
        return bookClosingDao.getClosingById(id)
    }

    suspend fun isDateClosed(bookId: Int, timestamp: Long): Boolean {
        return bookClosingDao.isDateClosed(bookId, timestamp) > 0
    }

    suspend fun closeBook(
            bookId: Int,
            periodStart: Long,
            periodEnd: Long,
            periodLabel: String,
            finalBalance: Double,
            isVerified: Boolean = false,
            notes: String = ""
    ): Long {
        val closing =
                BookClosing(
                        bookId = bookId,
                        periodStart = periodStart,
                        periodEnd = periodEnd,
                        periodLabel = periodLabel,
                        finalBalance = finalBalance,
                        isVerified = isVerified,
                        notes = notes,
                        closedAt = System.currentTimeMillis(),
                        isSynced = false,
                        syncAction = "CREATE"
                )
        return bookClosingDao.insert(closing)
    }

    suspend fun reopenBook(closingId: Int) {
        // Soft delete to reopen the period
        bookClosingDao.softDelete(closingId)
    }

    suspend fun deleteClosing(closing: BookClosing) {
        // Use soft delete by default for sync safety, or hard delete if really intended?
        // Existing implementation was hard delete. Let's switch to soft delete if we want sync
        // deletion propagation.
        // But the previous implementation called `delete` which was HARD delete in Dao.
        // Let's use softDelete
        bookClosingDao.softDelete(closing.id)
    }

    // --- SYNC REPOSITORY IMPLEMENTATION ---
    override suspend fun getAllUnsynced(): List<BookClosing> = bookClosingDao.getAllUnsynced()

    override suspend fun getByServerId(serverId: String): BookClosing? =
            bookClosingDao.getByServerId(serverId)

    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        bookClosingDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    override suspend fun deleteByIdPermanently(id: Long) {
        bookClosingDao.deleteByIdPermanently(id.toInt())
    }

    override suspend fun saveFromRemote(entity: BookClosing) {
        // Check if exists by server ID first
        val existing =
                if (entity.serverId != null) bookClosingDao.getByServerId(entity.serverId) else null

        if (existing != null) {
            // Update
            val updated = entity.copy(id = existing.id, isSynced = true, syncAction = null)
            bookClosingDao.update(updated)
        } else {
            // Insert
            val newEntity = entity.copy(id = 0, isSynced = true, syncAction = null)
            bookClosingDao.insert(newEntity)
        }
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        bookClosingDao.markAsUnsynced(id.toInt(), action)
    }
}
