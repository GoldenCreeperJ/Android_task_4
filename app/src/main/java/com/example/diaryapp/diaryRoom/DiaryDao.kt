package com.example.diaryapp.diaryRoom

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DiaryDao {

    @Insert
    fun insert(diaryEntity: DiaryEntity)

    @Update
    fun update(diaryEntity: DiaryEntity)

    @Delete
    fun delete(diaryEntity: DiaryEntity)

    @Query("select * from DiaryEntity")
    fun getAllData(): List<DiaryEntity>

    @Query("select * from DiaryEntity where date = :date")
    fun getOneData(date: String): DiaryEntity?
}