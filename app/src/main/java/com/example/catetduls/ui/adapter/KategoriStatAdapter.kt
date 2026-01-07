package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.CategoryExpense
import com.google.android.material.card.MaterialCardView
import com.example.catetduls.utils.Formatters

import java.util.Locale

class CategoryStatAdapter(
    private val onItemClick: ((CategoryExpense) -> Unit)? = null,
    private var currencySymbol: String = "Rp" // Default
) : RecyclerView.Adapter<CategoryStatAdapter.ViewHolder>() {

    private var items: List<CategoryExpense> = emptyList()
    private var totalAmount: Double = 1.0

    private val colors = listOf(
        "#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0",
        "#9966FF", "#FF9F40", "#33FFCC", "#C9CBCF"
    )

    fun setCurrency(symbol: String) {
        this.currencySymbol = symbol
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<CategoryExpense>) {
        items = newItems
        // Hitung total untuk persentase
        totalAmount = newItems.sumOf { it.total }
        if (totalAmount == 0.0) totalAmount = 1.0

        // Debugging: Cek apakah data masuk
        android.util.Log.d("DEBUG_ADAPTER", "Submit List: ${items.size} items. Total: $totalAmount")

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_stat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position, totalAmount, colors, currencySymbol, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardBadge: MaterialCardView = itemView.findViewById(R.id.card_percent_badge)
        private val tvPercent: TextView = itemView.findViewById(R.id.tv_percent)
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_category_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvTotal: TextView = itemView.findViewById(R.id.tv_category_total)

        fun bind(
            item: CategoryExpense,
            position: Int,
            grandTotal: Double,
            colors: List<String>,
            currencySymbol: String,
            onItemClick: ((CategoryExpense) -> Unit)?
        ) {
            tvIcon.text = item.icon
            tvName.text = item.categoryName

            val percentage = (item.total / grandTotal) * 100
            tvPercent.text = String.format("%.0f%%", percentage)

            val colorHex = colors[position % colors.size]
            cardBadge.setCardBackgroundColor(android.graphics.Color.parseColor(colorHex))

            // Gunakan CurrencyHelper.format dengan symbol dinamis
            tvTotal.text = com.example.catetduls.utils.CurrencyHelper.format(item.total, currencySymbol)

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}