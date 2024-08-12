package com.example.diaryapp.diaryRoom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(version = 1, entities = [DiaryEntity::class], exportSchema = false)
abstract class DiaryDatabase: RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        private var instance: DiaryDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): DiaryDatabase {
            instance?.let {
                return it
            }
            return Room.databaseBuilder(
                context.applicationContext,
                DiaryDatabase::class.java, "DiaryDataBase"
            )
                .allowMainThreadQueries()
                .build().apply { instance = this }
        }
    }
}