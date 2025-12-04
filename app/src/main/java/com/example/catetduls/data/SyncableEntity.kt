package com.example.catetduls.data

interface SyncableEntity {
    val id: Int
    val serverId: String?
    val isSynced: Boolean
    val isDeleted: Boolean
    val syncAction: String?
    val lastSyncAt: Long?
    val updatedAt: Long
}