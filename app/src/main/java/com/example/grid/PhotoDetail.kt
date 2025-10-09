package com.example.grid

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.example.grid.databinding.ActivityPhotoDetailBinding

class PhotoDetail : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("PHOTO_TITLE")
        val imageUrl = intent.getStringExtra("PHOTO_IMAGE_URL")

        binding.ivDetailPhoto.load(imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
        }

        binding.tvDetailTitle.text = title
    }
}