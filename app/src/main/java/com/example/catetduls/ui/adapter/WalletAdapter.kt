package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Wallet
import java.text.NumberFormat
import java.util.Locale

class WalletAdapter(private val onEdit: (com.example.catetduls.data.Wallet) -> Unit) :
        ListAdapter<com.example.catetduls.data.WalletWithStats, WalletAdapter.ViewHolder>(
                WalletDiffCallback()
        ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
                LayoutInflater.from(parent.context)
                        .inflate(
                                R.layout.item_kategori,
                                parent,
                                false
                        ) // Kita gunakan layout item_kategori sementara agar hemat, atau buat
        // item_wallet.xml baru
        return ViewHolder(view, onEdit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View, private val onEdit: (Wallet) -> Unit) :
            RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvType: TextView =
                itemView.findViewById(R.id.tv_type) // Bisa dipakai untuk menampilkan Saldo
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)

        fun bind(walletWithStats: com.example.catetduls.data.WalletWithStats) {
            val wallet = walletWithStats.wallet
            tvIcon.text = wallet.icon
            tvName.text = wallet.name

            // Format Saldo - menggunakan netBalance yang dihitung dari transaksi
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0
            tvType.text = formatter.format(walletWithStats.netBalance)

            btnEdit.setOnClickListener { onEdit(wallet) }
        }
    }

    class WalletDiffCallback : DiffUtil.ItemCallback<com.example.catetduls.data.WalletWithStats>() {
        override fun areItemsTheSame(
                oldItem: com.example.catetduls.data.WalletWithStats,
                newItem: com.example.catetduls.data.WalletWithStats
        ): Boolean = oldItem.wallet.id == newItem.wallet.id
        override fun areContentsTheSame(
                oldItem: com.example.catetduls.data.WalletWithStats,
                newItem: com.example.catetduls.data.WalletWithStats
        ): Boolean = oldItem == newItem
    }
}
