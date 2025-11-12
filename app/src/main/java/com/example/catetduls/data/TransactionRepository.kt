package com.example.catetduls.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository adalah perantara antara ViewModel dan DAO.
 * Ini adalah "best practice" arsitektur MVVM.
 * ViewModel HANYA boleh tahu tentang Repository, tidak boleh tahu tentang DAO.
 *
 * (Nanti kita akan menggunakan Hilt untuk 'meng-inject' transactionDao ke sini)
 */
class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    // Fungsi-fungsi ini hanya "meneruskan" panggilan ke DAO.
    // Jika nanti kita butuh mengambil data dari API (online),
    // logikanya akan ditambahkan di sini.

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    fun getTransactionById(id: Int): Flow<Transaction?> =
        transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    // --- Fungsi Dashboard ---

    fun getTotalBalance(): Flow<Double?> =
        transactionDao.getTotalBalance()

    fun getRecentTransactions(): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions()
}