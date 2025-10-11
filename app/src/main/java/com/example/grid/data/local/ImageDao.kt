package com.example.grid.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao   //  Data Access Object
interface ImageDao {
    // 모든 이미지들을 id 내림차순(최신순)으로 가져오기
    @Query("SELECT * FROM images ORDER BY id DESC")
    suspend fun getAllImages(): List<ImageEntity>

    // 이미지 1개 추가하기
    @Insert
    suspend fun insertImage(image: ImageEntity)
}