package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class MemoRepository @Inject constructor(private val memoDao: MemoDao) : SyncRepository<Memo> {

    fun getAllMemosByBook(bookId: Int): Flow<List<Memo>> {
        return memoDao.getAllMemosByBook(bookId)
    }

    fun getMemosByTag(bookId: Int, tag: String): Flow<List<Memo>> {
        return memoDao.getMemosByTag(bookId, tag)
    }

    suspend fun getMemoById(id: Int): Memo? {
        return memoDao.getMemoById(id)
    }

    suspend fun saveMemo(
            bookId: Int,
            title: String,
            content: String,
            tags: String = "",
            date: Long = System.currentTimeMillis()
    ): Long {
        val memo =
                Memo(
                        bookId = bookId,
                        title = title,
                        content = content,
                        tags = tags,
                        date = date,
                        isSynced = false,
                        syncAction = "CREATE"
                )
        return memoDao.insert(memo)
    }

    suspend fun updateMemo(id: Int, title: String, content: String, tags: String) {
        val existing = memoDao.getMemoById(id)
        if (existing != null) {
            val updated =
                    existing.copy(
                            title = title,
                            content = content,
                            tags = tags,
                            updatedAt = System.currentTimeMillis(),
                            syncAction = "UPDATE"
                    )
            memoDao.update(updated)
        }
    }

    suspend fun deleteMemo(id: Int) {
        memoDao.softDelete(id)
    }

    // --- SYNC REPOSITORY IMPLEMENTATION ---
    override suspend fun getAllUnsynced(): List<Memo> = memoDao.getAllUnsynced()

    override suspend fun getByServerId(serverId: String): Memo? = memoDao.getByServerId(serverId)

    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        memoDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    override suspend fun deleteByIdPermanently(id: Long) {
        memoDao.deleteByIdPermanently(id.toInt())
    }

    override suspend fun saveFromRemote(entity: Memo) {
        val existing = if (entity.serverId != null) memoDao.getByServerId(entity.serverId) else null

        if (existing != null) {
            val updated = entity.copy(id = existing.id, isSynced = true, syncAction = null)
            memoDao.update(updated)
        } else {
            val newEntity = entity.copy(id = 0, isSynced = true, syncAction = null)
            memoDao.insert(newEntity)
        }
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        memoDao.markAsUnsynced(id.toInt(), action)
    }
}
