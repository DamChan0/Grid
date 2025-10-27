package com.example.grid.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * FolderEntity
 * -------------
 * Room 데이터베이스에 저장되는 폴더 정보 클래스.
 * 이미지를 폴더별로 분류하여 관리하기 위한 엔티티입니다.
 * 
 * @property id 폴더 고유 ID (자동 생성)
 * @property name 폴더 이름 (예: "Pictures", "DCIM", "Download")
 * @property timestamp 폴더 생성 시간 (밀리초 단위)
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    
    val name: String,
    
    val timestamp: Long = System.currentTimeMillis()
)

