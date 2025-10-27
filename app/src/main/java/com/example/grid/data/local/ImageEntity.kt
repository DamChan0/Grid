package com.example.grid.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ImageEntity
 * -------------
 * Room 데이터베이스에 저장되는 로컬 이미지 정보 클래스.
 * 사용자가 갤러리에서 선택한 사진을 앱 내부 DB에 저장할 때 사용됩니다.
 * 
 * @property folderId 이미지가 속한 폴더의 ID (FolderEntity와 연결)
 * 
 * Foreign Key:
 * - 폴더가 삭제되면 해당 폴더의 모든 이미지도 자동 삭제 (CASCADE)
 * - folderId는 반드시 존재하는 폴더 ID여야 함
 */
@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE  // 폴더 삭제 시 이미지도 함께 삭제
        )
    ],
    indices = [Index(value = ["folderId"])]  // 폴더별 조회 성능 향상
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // 이미지 제목
    val title: String,

    // 로컬 파일 URI (content:// 형식)
    val imageUri: String,

    // 설명 (optional)
    val description: String? = null,

    // 이미지가 속한 폴더 ID (필수)
    val folderId: Int,

    // 이미지가 추가된 시간 (정렬, 정렬 필터용)
    val timestamp: Long = System.currentTimeMillis()
)