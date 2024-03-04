package com.example.diaryapp.diaryRoom

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class DiaryEntity(var title:String, var city:String, var weather:String,@PrimaryKey var date: String)