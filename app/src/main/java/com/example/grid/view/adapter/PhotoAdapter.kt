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

        android.util.Log.d(
            "PhotoAdapter",
            "Binding view for position $position, title: ${photo.title}, uri: ${photo.imageUri}"
        )

        holder.binding.tvTitle.text = photo.title

        // URI를 Uri 객체로 변환하여 로드
        val uri = android.net.Uri.parse(photo.imageUri)
        
        // Photo Picker URI인 경우 권한 확인
        if (photo.imageUri.contains("picker_get_content")) {
            android.util.Log.d("PhotoAdapter", "Photo Picker URI detected: $uri")
        }
        
        holder.binding.ivPhoto.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_menu_gallery)
            error(R.drawable.ic_menu_gallery)
            listener(
                onStart = { request ->
                    android.util.Log.d("PhotoAdapter", "Loading image: $uri")
                },
                onSuccess = { request, result ->
                    android.util.Log.d("PhotoAdapter", "Image loaded successfully: $uri")
                },
                onError = { request, result ->
                    android.util.Log.e("PhotoAdapter", "Failed to load image: $uri", result.throwable)
                    // Photo Picker URI 권한 문제인 경우 특별 처리
                    if (result.throwable is SecurityException) {
                        android.util.Log.e("PhotoAdapter", "SecurityException - URI permission issue for Photo Picker")
                    }
                }
            )
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
            android.util.Log.d(
                "PhotoAdapter",
                "Starting PhotoDetail with title: ${photo.title}"
            )

            context.startActivity(intent)
        }

    }


}