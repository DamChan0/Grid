package com.example.grid.data.local

import androidx.room.Dao
import androidx.room.Delete
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

    // 여러 id에 해당하는 레코드를 한 번에 삭제
    @Query("DELETE FROM images WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    // 엔티티 리스트로 삭제할 때 사용하는 편의 메서드
    @Delete
    suspend fun deleteImages(images: List<ImageEntity>)
}