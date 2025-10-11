package com.example.grid.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.grid.databinding.ActivityPhotoDetailBinding

class PhotoDetail : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // PhotoAdapter에서 전달한 개별 데이터 받기
        val title = intent.getStringExtra("TITLE")
        val imageUri = intent.getStringExtra("URI")
        val description = intent.getStringExtra("DESCRIPTION")
        val id = intent.getIntExtra("ID", 0)
        val timestamp = intent.getLongExtra("TIMESTAMP", 0L)

        // 이미지 로드
        val uri = android.net.Uri.parse(imageUri)
        binding.ivDetailPhoto.load(uri) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
            listener(
                onStart = { request -> android.util.Log.d("PhotoDetail", "Loading image: $uri") },
                onSuccess = { request, result -> android.util.Log.d("PhotoDetail", "Image loaded successfully: $uri") },
                onError = { request, result -> android.util.Log.e("PhotoDetail", "Failed to load image: $uri", result.throwable) }
            )
        }

        // 텍스트 설정
        binding.tvDetailTitle.text = title ?: "제목 없음"
        binding.tvDescription.text = description ?: "설명 없음"

        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}