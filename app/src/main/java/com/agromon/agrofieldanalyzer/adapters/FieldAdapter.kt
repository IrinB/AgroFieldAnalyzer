package com.agromon.agrofieldanalyzer.adapters

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.TextView
import android.view.View
import com.agromon.agrofieldanalyzer.R
import com.agromon.agrofieldanalyzer.model.Field

class FieldAdapter(
    private val onFieldClick: (Field) -> Unit,
    private val onCameraClick: (Field) -> Unit
) : RecyclerView.Adapter<FieldAdapter.ViewHolder>() {

    private var fields = listOf<Field>()

    fun submitList(newList: List<Field>) {
        fields = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_field, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fields[position])
    }

    override fun getItemCount(): Int = fields.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFieldName: TextView = itemView.findViewById(R.id.tvFieldName)
        private val tvFieldSizeValue: TextView = itemView.findViewById(R.id.tvFieldSizeValue)
        private val btnCamera: ImageButton = itemView.findViewById(R.id.btnCamera)

        fun bind(field: Field) {
            tvFieldName.text = field.name
            tvFieldSizeValue.text = "${field.area} га"

            itemView.setOnClickListener {
                onFieldClick(field)
            }

            btnCamera.setOnClickListener {
                onCameraClick(field)
            }
        }
    }
}