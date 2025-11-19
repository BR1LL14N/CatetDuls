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

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val getCategoryName: (Int) -> String = { "" },
    private val getCategoryIcon: (Int) -> String = { "💰" }
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction, onItemClick, getCategoryName, getCategoryIcon)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconContainer: FrameLayout = itemView.findViewById(R.id.icon_container)
        private val tvCategoryIcon: TextView = itemView.findViewById(R.id.tv_category_icon)
        private val tvTransactionTitle: TextView = itemView.findViewById(R.id.tv_transaction_title)
        private val tvTransactionSource: TextView = itemView.findViewById(R.id.tv_transaction_source)
        private val tvTransactionId: TextView = itemView.findViewById(R.id.tv_transaction_id)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_datetime)

        fun bind(
            transaction: Transaction,
            onItemClick: (Transaction) -> Unit,
            getCategoryName: (Int) -> String,
            getCategoryIcon: (Int) -> String
        ) {
            val context = itemView.context
            val categoryName = getCategoryName(transaction.categoryId)
            val categoryIcon = getCategoryIcon(transaction.categoryId)

            // Set category icon
            tvCategoryIcon.text = categoryIcon

            // Set icon container background based on transaction type
            val iconBg = when (transaction.type) {
                TransactionType.PEMASUKAN -> R.drawable.bg_icon_container_green
                TransactionType.PENGELUARAN -> R.drawable.bg_icon_container_red
            }
            iconContainer.setBackgroundResource(iconBg)

            // Set transaction title (category name)
            tvTransactionTitle.text = categoryName

            // Set transaction source/destination
            val source = if (transaction.notes.isNotEmpty()) {
                transaction.notes
            } else {
                when (transaction.type) {
                    TransactionType.PEMASUKAN -> "From $categoryName"
                    TransactionType.PENGELUARAN -> "Purchase from $categoryName"
                }
            }
            tvTransactionSource.text = source

            // Set transaction ID
            tvTransactionId.text = "Transaction ID ${transaction.id}${System.currentTimeMillis()}"

            // Set amount with currency format
            val amount = formatCurrency(transaction.amount)
            tvAmount.text = amount

            // Set amount color based on type
            val amountColor = when (transaction.type) {
                TransactionType.PEMASUKAN -> ContextCompat.getColor(context, R.color.success)
                TransactionType.PENGELUARAN -> ContextCompat.getColor(context, R.color.text_primary)
            }
            tvAmount.setTextColor(amountColor)

            // Set status badge
            val status = getTransactionStatus(transaction)
            tvStatus.text = status.name.lowercase()

            when (status) {
                TransactionStatus.CONFIRMED -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_confirmed)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.confirmed))
                }
                TransactionStatus.PENDING -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.pending))
                }
                TransactionStatus.CANCELLED -> {
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_cancelled)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.cancelled))
                }
            }

            // Set date time
            tvDateTime.text = formatDateTime(transaction.date)

            // Set click listener
            itemView.setOnClickListener { onItemClick(transaction) }
        }

        private fun formatCurrency(amount: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            return format.format(amount).replace("Rp", "Rp ")
        }

        private fun formatDateTime(timestamp: Long): String {
            val date = Date(timestamp)
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return "${dateFormat.format(date)}\n${timeFormat.format(date)}"
        }

        private fun getTransactionStatus(transaction: Transaction): TransactionStatus {
            // Logic untuk menentukan status
            // Untuk saat ini, semua transaksi dianggap confirmed
            // Anda bisa menambahkan field status di Transaction entity
            val daysDiff = (System.currentTimeMillis() - transaction.date) / (1000 * 60 * 60 * 24)

            return when {
                daysDiff < 1 -> TransactionStatus.PENDING
                transaction.amount < 0 -> TransactionStatus.CANCELLED
                else -> TransactionStatus.CONFIRMED
            }
        }
    }

    enum class TransactionStatus {
        CONFIRMED,
        PENDING,
        CANCELLED
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}