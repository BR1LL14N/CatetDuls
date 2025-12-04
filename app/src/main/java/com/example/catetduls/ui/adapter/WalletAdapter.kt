package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Wallet
import java.text.NumberFormat
import java.util.Locale

class WalletAdapter(
    private val onEdit: (Wallet) -> Unit
) : ListAdapter<Wallet, WalletAdapter.ViewHolder>(WalletDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori, parent, false) // Kita gunakan layout item_kategori sementara agar hemat, atau buat item_wallet.xml baru
        return ViewHolder(view, onEdit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onEdit: (Wallet) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type) // Bisa dipakai untuk menampilkan Saldo
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)

        fun bind(wallet: Wallet) {
            tvIcon.text = wallet.icon
            tvName.text = wallet.name

            // Format Saldo
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0
            tvType.text = formatter.format(wallet.currentBalance)

            btnEdit.setOnClickListener { onEdit(wallet) }
        }
    }

    class WalletDiffCallback : DiffUtil.ItemCallback<Wallet>() {
        override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean = oldItem == newItem
    }
}