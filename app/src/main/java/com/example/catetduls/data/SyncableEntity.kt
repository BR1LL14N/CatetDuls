package com.example.catetduls.data


interface SyncableEntity {
    val id: Int
    val serverId: String?
    val syncAction: String?
    val isSynced: Boolean
    val isDeleted: Boolean
    val updatedAt: Long

    val lastSyncAt: Long
}
