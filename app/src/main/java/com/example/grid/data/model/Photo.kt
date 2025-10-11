package com.example.grid.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Photo(
    val title: String, val imageUrl: String, val content: String = "기본 내용입니다."
) : Parcelable
