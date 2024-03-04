package com.example.diaryapp.viewModel

import androidx.lifecycle.ViewModel
import com.example.diaryapp.diaryRoom.DiaryDatabase
import com.example.diaryapp.diaryRoom.DiaryEntity

class DiaryViewModel : ViewModel() {
    private val diaryDao=DiaryDatabase.getDatabase(DiaryApplication.context).diaryDao()
    var adapterDiary= diaryDao.getAllData()
    var allDiary= diaryDao.getAllData()
    var oneDiary= DiaryEntity("","","","")
    var diaryData=""
    var filterWords=""
    var mode=0

    fun setOneDiary(date:String){
         if(diaryDao.getOneData(date)!=null) oneDiary =diaryDao.getOneData(date)!!
         else {
             oneDiary=DiaryEntity("${date}的日记","未知","未知",date)
             diaryDao.insert(oneDiary)}}

    fun setFilteredTitle() {
        adapterDiary= if (filterWords.isNotEmpty()) allDiary.filter { it.title.contains(filterWords) }
                    else allDiary}

    fun deleteDiary() {
        diaryDao.delete(oneDiary)
        allDiary = diaryDao.getAllData()}

    fun updateDiary() {
        if(diaryDao.getOneData(oneDiary.date)!=null) diaryDao.update(oneDiary)
        allDiary = diaryDao.getAllData() }
}