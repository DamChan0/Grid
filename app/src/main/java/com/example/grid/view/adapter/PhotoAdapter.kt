package com.example.grid.view.adapter

import android.R
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.grid.data.model.Photo
import com.example.grid.databinding.ItemPhotoBinding
import com.example.grid.view.PhotoDetail
import com.example.grid.data.local.ImageEntity


class PhotoAdapter(private val photoList: List<ImageEntity>) :
    RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false // false :RecyclerView가  알아서 처리한다. 바로 attatch 할 필요 없다는 의미
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return photoList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val photo = photoList[position]

        holder.binding.tvTitle.text = photo.title
        holder.binding.ivPhoto.load(photo.imageUri) {
            crossfade(true)
            placeholder(R.drawable.ic_menu_gallery)
        }

        holder.binding.ivPhoto.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PhotoDetail::class.java)
            intent.putExtra("TITLE", photo.title)
            intent.putExtra("URI", photo.imageUri)
            intent.putExtra("DESCRIPTION", photo.description)
            intent.putExtra("ID", photo.id)
            intent.putExtra("TIMESTAMP", photo.timestamp)
            
            // 디버깅용 로그
            android.util.Log.d("PhotoAdapter", "Starting PhotoDetail with title: ${photo.title}")
            
            context.startActivity(intent)
        }

    }


}