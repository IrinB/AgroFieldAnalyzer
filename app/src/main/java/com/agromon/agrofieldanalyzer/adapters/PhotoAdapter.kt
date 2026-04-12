package com.agromon.agrofieldanalyzer.adapters

import com.bumptech.glide.Glide
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.agromon.agrofieldanalyzer.R
import com.agromon.agrofieldanalyzer.model.Photo

class PhotoAdapter(
    private val onAddClick: () -> Unit,
    private val onPhotoClick: (Photo) -> Unit,
    private val onDeleteClick: (Photo) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    private var photos = listOf<Photo>()

    fun submitList(newList: List<Photo>) {
        photos = newList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = photos.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            holder.bindAdd()
        } else {
            holder.bindPhoto(photos[position - 1])
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val addIcon: ImageView = itemView.findViewById(R.id.ivAddIcon)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeletePhoto)

        fun bindAdd() {
            imageView.setImageDrawable(null)
            addIcon.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE  // ← добавьте эту строку
            itemView.setOnClickListener { onAddClick() }
        }

        fun bindPhoto(photo: Photo) {
            addIcon.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE

            Glide.with(itemView.context)
                .load(photo.photoUri)
                .placeholder(R.drawable.ic_photo_placeholder)
                .into(imageView)

            itemView.setOnClickListener { onPhotoClick(photo) }

            // Убедитесь, что эта строка есть и НЕ закомментирована
            btnDelete.setOnClickListener {
                onDeleteClick(photo)
            }
        }
    }
}