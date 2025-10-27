package com.example.grid.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.grid.data.local.FolderEntity
import com.example.grid.databinding.ItemFolderBinding

/**
 * FolderAdapter
 * -------------
 * 폴더 목록을 RecyclerView에 표시하는 어댑터
 *
 * @param folderList 표시할 폴더 리스트
 * @param imageCountMap 각 폴더의 이미지 개수 맵 (folderId -> count)
 * @param onFolderClick 폴더 클릭 리스너
 * @param onFolderLongClick 폴더 롱클릭 리스너
 */
class FolderAdapter(
    private val folderList: MutableList<FolderEntity>,
    private val imageCountMap: MutableMap<Int, Int> = mutableMapOf(),
    private val onFolderClick: (FolderEntity) -> Unit,
    private val onFolderLongClick: (FolderEntity) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    /**
     * ViewHolder - 폴더 아이템의 뷰를 담는 홀더
     */
    inner class FolderViewHolder(val binding: ItemFolderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init { // ✅ 깔끔하게 함수 호출만
            binding.root.setOnClickListener { handleClick() }
            binding.root.setOnLongClickListener { handleLongClick() }
        }

        /**
         * 폴더 클릭 처리
         */
        private fun handleClick() {
            val clickedPosition = bindingAdapterPosition
            if (clickedPosition != RecyclerView.NO_POSITION) {
                val folder = folderList[clickedPosition]
                onFolderClick(folder)
                android.util.Log.d("FolderAdapter", "Folder clicked: ${folder.name}")
            }
        }

        /**
         * 폴더 롱클릭 처리
         */
        private fun handleLongClick(): Boolean {
            val clickedPosition = bindingAdapterPosition
            return if (clickedPosition != RecyclerView.NO_POSITION) {
                val folder = folderList[clickedPosition]
                onFolderLongClick(folder)
                android.util.Log.d(
                    "FolderAdapter", "Folder long-clicked: ${folder.name}"
                )
                true
            } else {
                false
            }
        }
    }

    /**
     * ViewHolder 생성 - XML 레이아웃을 inflate하여 ViewHolder 생성
     */
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        android.util.Log.d("FolderAdapter", "onCreateViewHolder 호출")
        return FolderViewHolder(binding)
    }

    /**
     * 아이템 개수 반환
     */
    override fun getItemCount(): Int {
        val count = folderList.size
        android.util.Log.d("FolderAdapter", "getItemCount: $count")
        return count
    }

    /**
     * 데이터를 뷰에 바인딩
     */
    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folderList[position]

        // 폴더 이름 설정
        holder.binding.tvFolderName.text = folder.name

        // 이미지 개수 설정
        val imageCount = imageCountMap[folder.id] ?: 0
        holder.binding.tvImageCount.text = "${imageCount}개"

        android.util.Log.d(
            "FolderAdapter",
            "onBindViewHolder: position=$position, name=${folder.name}, count=$imageCount"
        )
    }

    /**
     * 특정 폴더의 이미지 개수 업데이트
     */
    fun updateImageCount(folderId: Int, count: Int) {
        imageCountMap[folderId] = count
        val position = folderList.indexOfFirst { it.id == folderId }
        if (position != -1) {
            notifyItemChanged(position)
            android.util.Log.d(
                "FolderAdapter",
                "Image count updated: folderId=$folderId, count=$count"
            )
        }
    }

    /**
     * 폴더 추가
     */
    fun addFolder(folder: FolderEntity) {
        folderList.add(0, folder)
        notifyItemInserted(0)
        android.util.Log.d("FolderAdapter", "Folder added: ${folder.name}")
    }

    /**
     * 폴더 삭제
     */
    fun removeFolder(folder: FolderEntity) {
        val position = folderList.indexOf(folder)
        if (position != -1) {
            folderList.removeAt(position)
            notifyItemRemoved(position)
            android.util.Log.d("FolderAdapter", "Folder removed: ${folder.name}")
        }
    }
}

