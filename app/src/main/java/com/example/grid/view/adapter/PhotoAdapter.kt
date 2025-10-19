package com.example.grid.view.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.grid.databinding.ItemPhotoBinding
import com.example.grid.view.PhotoDetail
import com.example.grid.data.local.ImageEntity

// photoList를 MutableList로 받아 내부에서 수정할 수 있게 변경했습니다.
class PhotoAdapter(
    val photoList: MutableList<ImageEntity>,
    private val onItemLongClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit,
) : RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    var multiSelectMode: Boolean = false


    inner class ViewHolder(val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnLongClickListener { _ ->
                val clickPos = bindingAdapterPosition
                if (clickPos == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener false
                } else {
                    onItemLongClick(clickPos)
                    true
                }
            }
        }
    }

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
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
            listener(onStart = { _ ->
                android.util.Log.d("PhotoAdapter", "Loading image: $uri")
            }, onSuccess = { _, _ ->
                android.util.Log.d(
                    "PhotoAdapter", "Image loaded successfully: $uri"
                )
            }, onError = { _, result ->
                android.util.Log.e(
                    "PhotoAdapter", "Failed to load image: $uri", result.throwable
                ) // Photo Picker URI 권한 문제인 경우 특별 처리
                if (result.throwable is SecurityException) {
                    android.util.Log.e(
                        "PhotoAdapter",
                        "SecurityException - URI permission issue for Photo Picker"
                    )
                }
            })
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
                "PhotoAdapter", "Starting PhotoDetail with title: ${photo.title}"
            )

            context.startActivity(intent)
        }

        // Apply background based on selection state
        val bgColor: Int = if (selectedItems.contains(position)) {
            android.graphics.Color.LTGRAY
        } else {
            android.graphics.Color.TRANSPARENT
        }
        holder.binding.root.setBackgroundColor(bgColor)

        // Toggle selection on root click
        holder.binding.root.setOnClickListener {
            if (multiSelectMode) {
                toggleSelection(position)
                onItemClick(position)
            } else {
                onItemClick(position)
            }
        }

    }

    // 선택된 포지션을 반환합니다 (읽기 전용 복사본)
    fun getSelectedItems(): Set<Int> = selectedItems.toSet()

    // 선택 토글
    fun toggleSelection(position: Int) {
        if (position !in 0 until photoList.size) return
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        notifyItemChanged(position)
    }

    // 선택 모두 해제
    fun clearSelection() {
        if (selectedItems.isEmpty()) return
        val copy = selectedItems.toList()
        selectedItems.clear()
        copy.forEach { pos ->
            if (pos in 0 until photoList.size) notifyItemChanged(pos)
        }
    }

    // 선택된 포지션들에 대응하는 항목들을 제거하고, 선택 상태를 초기화합니다.
    fun removeItemsAtPositions(positions: Set<Int>) {
        if (positions.isEmpty()) return // 인덱스는 낮은 순서부터 제거해야 인덱스 흔들림을 방지합니다.
        val sorted = positions.sortedDescending()
        sorted.forEach { idx ->
            if (idx in 0 until photoList.size) {
                photoList.removeAt(idx)
            }
        }
        selectedItems.clear()
        notifyDataSetChanged()
    }

}