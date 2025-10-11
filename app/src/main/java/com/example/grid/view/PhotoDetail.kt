package com.example.grid.view

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.grid.data.model.Photo
import com.example.grid.databinding.ActivityPhotoDetailBinding

class PhotoDetail : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoDetailBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photo = intent.getParcelableExtra("photo", Photo::class.java)

        binding.ivDetailPhoto.load(photo?.imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
        }

        binding.tvDetailTitle.text = photo?.title
        binding.tvDescription.text = photo?.content


    }
}