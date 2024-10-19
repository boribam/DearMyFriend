package com.bbam.dearmyfriend.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.data.Animal
import com.bbam.dearmyfriend.databinding.AnimalItemViewBinding
import com.bumptech.glide.Glide

class AnimalAdapter(private val animalList: List<Animal>) :
    RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder>() {

    inner class AnimalViewHolder(val binding: AnimalItemViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(animal: Animal) {
            binding.tvDateStart.text = animal.dateStart
            binding.tvDateEnd.text = animal.dateEnd
            binding.tvKind.text = animal.kind
            binding.tvAge.text = animal.age
            binding.tvHappenPlace.text = animal.happenPlace
            Glide.with(binding.root.context).load(animal.thumbnailUrl).into(binding.ivThumb)
            Log.d("AnimalAdapter", "Binding animal: ${animal.kind}, ${animal.age}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val binding = AnimalItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(animalList[position])
    }

    override fun getItemCount(): Int = animalList.size
}