package com.example.diaryapp.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider

class DiaryApplication: Application() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        lateinit var diaryViewModel:DiaryViewModel
    }

    override fun onCreate() {
        super.onCreate()
        context =applicationContext
        diaryViewModel=ViewModelProvider.AndroidViewModelFactory(this).create(DiaryViewModel::class.java)
    }
}