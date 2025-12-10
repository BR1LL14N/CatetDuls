package com.example.catetduls.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Transaction
import com.example.catetduls.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (com.example.catetduls.data.Transaction) -> Unit,
    private val getCategoryName: (Int) -> String = { "" },
    private val getCategoryIcon: (Int) -> String = { "💰" },
    private val getWalletName: (Int) -> String = { "" }
) : ListAdapter<TransactionListItem, RecyclerView.ViewHolder>(TransactionDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TransactionListItem.DateHeader -> TYPE_HEADER
            is TransactionListItem.TransactionItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TransactionListItem.DateHeader -> (holder as HeaderViewHolder).bind(item)
            is TransactionListItem.TransactionItem -> (holder as TransactionViewHolder).bind(
                item.transaction,
                onItemClick,
                getCategoryName,
                getCategoryIcon,
                getWalletName
            )
        }
    }

    // ==========================================
    // 1. VIEWHOLDER HEADER (TANGGAL & TOTAL)
    // ==========================================
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDateDay: TextView = itemView.findViewById(R.id.tv_date_day)
        private val tvDateDayName: TextView = itemView.findViewById(R.id.tv_date_day_name)
        private val tvDateMonthYear: TextView = itemView.findViewById(R.id.tv_date_month_year)
        private val tvDailyIncome: TextView = itemView.findViewById(R.id.tv_daily_income)
        private val tvDailyExpense: TextView = itemView.findViewById(R.id.tv_daily_expense)

        fun bind(header: TransactionListItem.DateHeader) {
            val date = Date(header.dateTimestamp)
            val localeID = Locale("id", "ID")

            tvDateDay.text = SimpleDateFormat("dd", localeID).format(date)
            tvDateDayName.text = SimpleDateFormat("EEE", localeID).format(date)
            tvDateMonthYear.text = SimpleDateFormat("MM.yyyy", localeID).format(date)

            val formatter = NumberFormat.getCurrencyInstance(localeID)
            formatter.maximumFractionDigits = 0

            if (header.dailyIncome > 0) {
                tvDailyIncome.text = formatter.format(header.dailyIncome).replace("Rp", "Rp ")
                tvDailyIncome.visibility = View.VISIBLE
            } else {
                tvDailyIncome.visibility = View.GONE
            }

            if (header.dailyExpense > 0) {
                tvDailyExpense.text = formatter.format(header.dailyExpense).replace("Rp", "Rp ")
                tvDailyExpense.visibility = View.VISIBLE
            } else {
                tvDailyExpense.visibility = View.GONE
            }
        }
    }

    // ==========================================
    // 2. VIEWHOLDER ITEM (TRANSAKSI)
    // ==========================================
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tv_category_icon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvNotes: TextView = itemView.findViewById(R.id.tv_transaction_notes)
        private val tvWalletName: TextView = itemView.findViewById(R.id.tv_wallet_name)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)

        private val cardImage: View = itemView.findViewById(R.id.card_image_preview)
        private val ivThumb: android.widget.ImageView = itemView.findViewById(R.id.iv_transaction_thumb)

        fun bind(
            transaction: com.example.catetduls.data.Transaction,
            onItemClick: (com.example.catetduls.data.Transaction) -> Unit,
            getCategoryName: (Int) -> String,
            getCategoryIcon: (Int) -> String,
            getWalletName: (Int) -> String
        ) {
            val context = itemView.context

            // 1. Set Icon & Nama Kategori
            tvCategoryIcon.text = getCategoryIcon(transaction.categoryId)
            tvCategoryName.text = getCategoryName(transaction.categoryId)

            // 2. Set Notes (Jika ada)
            if (transaction.notes.isNotBlank()) {
                tvNotes.text = transaction.notes
                tvNotes.visibility = View.VISIBLE
            } else {
                tvNotes.visibility = View.GONE
            }

            // 3. Set Nama Dompet (Tunai/Bank)
            tvWalletName.text = getWalletName(transaction.walletId)

            // 4. Set Amount & Warna
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0

            tvAmount.text = CurrencyUtils.toRupiah(transaction.amount)

            // ✅ FIXED: Tambahkan branch untuk TRANSFER
            val amountColor = when (transaction.type) {
                TransactionType.PEMASUKAN -> ContextCompat.getColor(context, R.color.success)
                TransactionType.PENGELUARAN -> ContextCompat.getColor(context, R.color.danger)
                TransactionType.TRANSFER -> ContextCompat.getColor(context, R.color.text_secondary) // Abu-abu untuk transfer
            }
            tvAmount.setTextColor(amountColor)

            // Click Listener
            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }

    // ==========================================
    // DIFF CALLBACK (UPDATED)
    // ==========================================
    class TransactionDiffCallback : DiffUtil.ItemCallback<TransactionListItem>() {
        override fun areItemsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TransactionListItem, newItem: TransactionListItem): Boolean {
            return oldItem == newItem
        }
    }

    object CurrencyUtils {
        fun toRupiah(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            format.maximumFractionDigits = 0
            return format.format(amount).replace("Rp", "Rp ")
        }
    }
}