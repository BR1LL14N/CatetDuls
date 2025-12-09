package com.example.catetduls.data

// Interface untuk repository yang mendukung sync
interface SyncRepository<T : SyncableEntity> {
    suspend fun getAllUnsynced(): List<T>
    suspend fun getByServerId(serverId: String): T?
    suspend fun updateSyncStatus(id: Long, serverId: String, syncedAt: Long)
    suspend fun deleteByIdPermanently(id: Long)
    suspend fun saveFromRemote(entity: T)
}