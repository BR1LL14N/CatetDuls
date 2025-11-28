package com.example.catetduls.ui.adapter

import com.example.catetduls.data.Transaction

sealed class TransactionListItem {

    // Tipe 1: Header Tanggal (Tgl 19, Rab, Pemasukan 50k, Pengeluaran 20k)
    data class DateHeader(
        val dateTimestamp: Long,
        val dailyIncome: Double,
        val dailyExpense: Double
    ) : TransactionListItem() {
        override val id: Long = dateTimestamp // ID unik untuk DiffUtil
    }

    // Tipe 2: Item Transaksi Asli
    data class TransactionItem(
        val transaction: Transaction
    ) : TransactionListItem() {
        override val id: Long = transaction.id.toLong()
    }

    abstract val id: Long
}