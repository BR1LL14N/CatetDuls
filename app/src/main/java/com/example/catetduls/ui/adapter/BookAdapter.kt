package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Book

class BookAdapter(
        private val onEdit: (Book) -> Unit,
        private val onDelete: (Book) -> Unit,
        private val onActivate: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.ViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return ViewHolder(view, onEdit, onDelete, onActivate)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
            itemView: View,
            val onEdit: (Book) -> Unit,
            val onDelete: (Book) -> Unit,
            val onActivate: (Book) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvIcon: TextView = itemView.findViewById(R.id.tv_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        private val tvActiveStatus: TextView = itemView.findViewById(R.id.tv_active_status)
        private val btnMore: ImageView = itemView.findViewById(R.id.btn_more)

        fun bind(book: Book) {
            tvIcon.text = book.icon
            tvName.text = book.name
            tvDescription.text = book.description

            if (book.isActive) {
                tvActiveStatus.visibility = View.VISIBLE
                tvActiveStatus.text = "Sedang Aktif"
            } else {
                tvActiveStatus.visibility = View.GONE
            }

            // Click listener for the entire item to activate (if not active)
            itemView.setOnClickListener {
                if (!book.isActive) {
                    onActivate(book)
                }
            }

            // More/Menu button
            btnMore.setOnClickListener { view -> showPopupMenu(view, book) }
        }

        private fun showPopupMenu(view: View, book: Book) {
            val popup = PopupMenu(view.context, view)
            popup.menu.add(0, 1, 0, "Aktifkan").apply { isVisible = !book.isActive }
            popup.menu.add(0, 2, 0, "Edit")
            popup.menu.add(0, 3, 0, "Hapus").apply {
                // Prevent deleting active book usually, but let calls decide logic
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        onActivate(book)
                        true
                    }
                    2 -> {
                        onEdit(book)
                        true
                    }
                    3 -> {
                        onDelete(book)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            return oldItem == newItem
        }
    }
}
