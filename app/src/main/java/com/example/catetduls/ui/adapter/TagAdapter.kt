package com.example.catetduls.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.catetduls.R
import com.example.catetduls.data.TagEntity

class TagAdapter(
    private val onEditClick: (TagEntity) -> Unit,
    private val onDeleteClick: (TagEntity) -> Unit
) : ListAdapter<TagEntity, TagAdapter.TagViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag_manage, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = getItem(position)
        holder.bind(tag)
    }

    inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_tag_name)
        private val ivColor: ImageView = itemView.findViewById(R.id.iv_tag_color)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btn_edit_tag)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btn_delete_tag)

        fun bind(tag: TagEntity) {
            tvName.text = tag.name
            
            try {
                val colorInt = if (tag.color.isNotEmpty()) Color.parseColor(tag.color) else Color.GRAY
                ivColor.imageTintList = ColorStateList.valueOf(colorInt)
            } catch (e: Exception) {
                ivColor.imageTintList = ColorStateList.valueOf(Color.GRAY)
            }

            btnEdit.setOnClickListener { onEditClick(tag) }
            btnDelete.setOnClickListener { onDeleteClick(tag) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TagEntity>() {
        override fun areItemsTheSame(oldItem: TagEntity, newItem: TagEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TagEntity, newItem: TagEntity) = oldItem == newItem
    }
}
