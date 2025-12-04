package com.example.catetduls.data

interface SyncRepository<T : SyncableEntity> {
    suspend fun getAllUnsynced(): List<T>
    suspend fun updateSyncStatus(localId: Int, serverId: String, lastSyncAt: Long)
    suspend fun deleteByIdPermanently(localId: Int)
    suspend fun saveFromRemote(entity: T)
    suspend fun getByServerId(serverId: String): T?
}