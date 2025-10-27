package com.example.grid.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/**
 * FolderDao
 * ----------
 * 폴더 데이터에 대한 데이터베이스 접근 인터페이스
 */
@Dao
interface FolderDao {
    
    /**
     * 모든 폴더를 최신순으로 가져옵니다.
     * @return 폴더 리스트 (최신 생성 순)
     */
    @Query("SELECT * FROM folders ORDER BY id DESC")
    suspend fun getAllFolders(): List<FolderEntity>
    
    /**
     * 새 폴더를 생성합니다.
     * @param folder 생성할 폴더 정보
     * @return 생성된 폴더의 ID
     */
    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long
    
    /**
     * 여러 폴더를 한 번에 삭제합니다.
     * @param ids 삭제할 폴더 ID 리스트
     */
    @Query("DELETE FROM folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)
    
    /**
     * 폴더 엔티티를 직접 삭제합니다.
     * @param folders 삭제할 폴더 리스트
     */
    @Delete
    suspend fun deleteFolders(folders: List<FolderEntity>)
    
    /**
     * 특정 폴더에 포함된 이미지 개수를 반환합니다.
     * @param folderId 조회할 폴더 ID
     * @return 해당 폴더의 이미지 개수
     */
    @Query("SELECT COUNT(*) FROM images WHERE folderId = :folderId")
    suspend fun getImageCountInFolder(folderId: Int): Int
    
    /**
     * 폴더 이름으로 검색합니다. (중복 확인용)
     * @param name 검색할 폴더 이름
     * @return 해당 이름의 폴더 (없으면 null)
     */
    @Query("SELECT * FROM folders WHERE name = :name LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?
    
    /**
     * 특정 ID의 폴더를 가져옵니다.
     * @param folderId 조회할 폴더 ID
     * @return 폴더 정보 (없으면 null)
     */
    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Int): FolderEntity?
}

