package com.example.grid.data.local

import android.media.Image
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [ImageEntity::class, FolderEntity::class],  // FolderEntity 추가
    version = 2,  // 버전 증가 (스키마 변경)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun imageDao(): ImageDao
    abstract fun folderDao(): FolderDao  // FolderDao 추가

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "image_database"
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // 디버깅용 (프로덕션에서는 제거)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}