package com.bbam.dearmyfriend.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.activity.PostWritingActivity
import com.bbam.dearmyfriend.databinding.ImgSelectItemBinding
import com.bumptech.glide.Glide

class PostPictureAdapter(val context: PostWritingActivity, val items: ArrayList<Uri>) : RecyclerView.Adapter<PostPictureAdapter.ViewHolder>() {

    interface onItemClickListener {
        fun onItemClick(position: Int)
    }
    private lateinit var itemClickListener: onItemClickListener
    fun setItemClickListener(itemClickListener: onItemClickListener) {
        this.itemClickListener = itemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ImgSelectItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindItems(item)
    }

    inner class ViewHolder(private val binding: ImgSelectItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindItems(item: Uri) {
            val imageArea = binding.imageArea
            val delete = binding.btnDelete

            delete.setOnClickListener {
                val position = adapterPosition
                itemClickListener.onItemClick(position)
            }

            Glide.with(context).load(item).into(imageArea)
        }
    }
}