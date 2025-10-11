package com.example.grid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ImageEntity
 * -------------
 * Room 데이터베이스에 저장되는 로컬 이미지 정보 클래스.
 * 사용자가 갤러리에서 선택한 사진을 앱 내부 DB에 저장할 때 사용됩니다.
 */
@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // 이미지 제목
    val title: String,

    // 로컬 파일 URI (content:// 형식)
    val imageUri: String,

    // 설명 (optional)
    val description: String? = null,

    // 이미지가 추가된 시간 (정렬, 정렬 필터용)
    val timestamp: Long = System.currentTimeMillis()
)