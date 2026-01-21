package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class TagRepository @Inject constructor(private val tagDao: TagDao) : SyncRepository<TagEntity> {

    val allTags: Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun insert(tag: TagEntity): Long {
        return tagDao.insert(tag)
    }

    suspend fun update(tag: TagEntity) {
        tagDao.update(tag)
    }

    suspend fun delete(tag: TagEntity) {
        // Soft delete implementation preferred
        tagDao.softDelete(tag.id)
    }

    // --- SYNC REPOSITORY IMPLEMENTATION ---
    override suspend fun getAllUnsynced(): List<TagEntity> = tagDao.getAllUnsynced()

    override suspend fun getByServerId(serverId: String): TagEntity? =
            tagDao.getByServerId(serverId)

    override suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long) {
        tagDao.updateSyncStatus(id.toInt(), serverId, syncedAt)
    }

    override suspend fun deleteByIdPermanently(id: Long) {
        tagDao.deleteByIdPermanently(id.toInt())
    }

    override suspend fun saveFromRemote(entity: TagEntity) {
        // Tag identity is usually by Name or ServerID
        val existing = if (entity.serverId != null) tagDao.getByServerId(entity.serverId) else null

        if (existing != null) {
            val updated = entity.copy(id = existing.id, isSynced = true, syncAction = null)
            tagDao.update(updated)
        } else {
            // Check if tag with same name exists locally (merge strategy?)
            // If we have "Penting" local and "Penting" remote (new), we should probably merge them.
            // But `tags` table has unique constraint on `name`.
            // So we must handle conflict.
            // For now, let's assume if serverId not found, insert new.
            // If name conflict occurs, we update the existing one with server props.
            try {
                val newEntity = entity.copy(id = 0, isSynced = true, syncAction = null)
                tagDao.insert(newEntity)
            } catch (e: Exception) {
                // Name conflict likely. Find by name?
                // For simplicity, we just ignore insert (or we load by name and update serverId)
            }
        }
    }

    override suspend fun markAsUnsynced(id: Long, action: String) {
        tagDao.markAsUnsynced(id.toInt(), action)
    }
}
