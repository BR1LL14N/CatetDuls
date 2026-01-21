package com.example.catetduls.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.Memo
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class MemoAdapter(
        private val onItemClick: (Memo) -> Unit,
        private val onItemLongClick: (Memo) -> Boolean
) : ListAdapter<Memo, MemoAdapter.MemoViewHolder>(MemoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_memo, parent, false)
        return MemoViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemoViewHolder(
            itemView: View,
            private val onItemClick: (Memo) -> Unit,
            private val onItemLongClick: (Memo) -> Boolean
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTitle: TextView = itemView.findViewById(R.id.tv_memo_title)
        private val tvContent: TextView = itemView.findViewById(R.id.tv_memo_content)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_memo_date)
        private val tvMemoTags: TextView = itemView.findViewById(R.id.tv_memo_tags)

        fun bind(memo: Memo) {
            tvTitle.text = memo.title
            tvContent.text = memo.content

            // Format date
            val dateFormat = SimpleDateFormat("dd MMM", Locale("id", "ID"))
            tvDate.text = dateFormat.format(Date(memo.date))

            // Show tags if not empty
            if (memo.tags.isNotBlank()) {
                tvMemoTags.visibility = View.VISIBLE
                // Format: #Tag1 • #Tag2
                val formattedTags = memo.tags.split(",").joinToString(" • ") { "#${it.trim()}" }
                tvMemoTags.text = formattedTags
            } else {
                tvMemoTags.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(memo) }
            itemView.setOnLongClickListener { onItemLongClick(memo) }
        }
    }

    class MemoDiffCallback : DiffUtil.ItemCallback<Memo>() {
        override fun areItemsTheSame(oldItem: Memo, newItem: Memo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Memo, newItem: Memo): Boolean {
            return oldItem == newItem
        }
    }
}
