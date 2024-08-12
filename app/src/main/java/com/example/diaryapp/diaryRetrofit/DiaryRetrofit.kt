package com.example.diaryapp.diaryRetrofit

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface DiaryRetrofit {
    @GET
    fun getNetPic(@Url picUrl: String): Call<ResponseBody>

    @GET("lookup")
    fun getCity(
        @Query("location") location: String,
        @Query("key") key: String = "cc27611044444a559ce1e33c8e2be837"
    ): Call<CityItem>

    @GET("now")
    fun getWeather(
        @Query("location") location: String,
        @Query("key") key: String = "cc27611044444a559ce1e33c8e2be837"
    ): Call<WeatherItem>
}