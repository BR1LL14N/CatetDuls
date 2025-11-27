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
import com.example.catetduls.data.Category

class KategoriAdapter(
    private val onDelete: (Category) -> Unit,
    private val onEdit: (Category) -> Unit
) : ListAdapter<Category, KategoriAdapter.ViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori, parent, false)
        return ViewHolder(view, onDelete, onEdit)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onDelete: (Category) -> Unit,
        private val onEdit: (Category) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvType: TextView = itemView.findViewById(R.id.tv_type)
        private val tvDefault: TextView = itemView.findViewById(R.id.tv_default)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)

        // file: KategoriAdapter.kt (di dalam class ViewHolder)

        fun bind(category: Category) {
            tvIcon.text = category.icon
            tvName.text = category.name
            tvType.text = category.type.name

            if (category.isDefault) {
                tvDefault.visibility = View.VISIBLE
            } else {
                tvDefault.visibility = View.GONE
            }

            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener { onEdit(category) }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}