package com.example.diaryapp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.example.diaryapp.DiaryApplication
import com.example.diaryapp.R
import com.example.diaryapp.databinding.ActivityDiaryBinding
import com.example.diaryapp.diaryRetrofit.CityItem
import com.example.diaryapp.diaryRetrofit.DiaryRetrofit
import com.example.diaryapp.diaryRetrofit.WeatherItem
import com.example.diaryapp.viewModel.DiaryViewModel
import com.google.android.material.navigation.NavigationBarView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class DiaryActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDiaryBinding
    private lateinit var diaryViewModel: DiaryViewModel
    private lateinit var watchNavigationListener:NavigationBarView.OnItemSelectedListener
    private lateinit var editNavigationListener:NavigationBarView.OnItemSelectedListener
    private lateinit var albumLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var diaryContentEditText:EditText
    private lateinit var imgFile: File

    @SuppressLint("SetTextI18n", "Recycle", "Range", "InlinedApi", "CommitPrefEdits", "SdCardPath")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityDiaryBinding.inflate(LayoutInflater.from(this))
        diaryViewModel= DiaryApplication.diaryViewModel
        diaryContentEditText=EditText(this).apply {
            setEms(10)
            labelFor=0
            touchDelegate=null
            accessibilityDelegate=null
            hint = "Write your diary"
            setBackgroundColor(Color.TRANSPARENT)
            importantForAccessibility= View.IMPORTANT_FOR_ACCESSIBILITY_NO
            minHeight= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48F,this.resources.displayMetrics).toInt() }

        setContentView(binding.root)

        watchNavigationListener = NavigationBarView.OnItemSelectedListener { item ->
            when(item.itemId){
                R.id.editButton ->{
                    saveDiary()
                    changeToEditView()
                    diaryContentEditText.setText(diaryViewModel.diaryData)
                    diaryViewModel.mode=1}

                R.id.deleteButton ->{
                    diaryViewModel.deleteDiary()
                    File(filesDir, diaryViewModel.oneDiary.date).delete()
                    File("/data/data/$packageName/shared_prefs/${diaryViewModel.oneDiary.date}.xml").delete()
                    finish()}

                R.id.backButton -> finish()

                R.id.outputButton ->{
                    permissionRequesterPlus(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                        Build.VERSION_CODES.TIRAMISU){
                        val imgDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "diary")
                        if (!imgDir.exists()) imgDir.mkdirs()

                        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, "${diaryViewModel.oneDiary.date}.jpg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/diary")
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")})!!

                        val bitmap = Bitmap.createBitmap(binding.diaryOutLayout.width, binding.diaryOutLayout.height, Bitmap.Config.ARGB_8888)
                        binding.diaryScrollView.draw(Canvas(bitmap).apply { drawColor(Color.WHITE) })

                        contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)}}}}
            true
        }

        editNavigationListener=NavigationBarView.OnItemSelectedListener { item ->
            when(item.itemId){
                R.id.finishButton ->{
                    saveDiary()
                    changeToWatchView()
                    setTextView()
                    diaryViewModel.mode=1}

                R.id.cameraButton->{
                    permissionRequester(Manifest.permission.CAMERA) {
                        imgFile=File(externalCacheDir,"${LocalDateTime.now().toString().replace(":","-")}.jpg").apply { createNewFile() }
                        cameraLauncher.launch(Intent("android.media.action.IMAGE_CAPTURE").apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this@DiaryActivity,
                                "com.example.diaryapp", imgFile))})}}

                R.id.albumButton ->{
                    permissionRequesterPlus(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_MEDIA_IMAGES,
                        Build.VERSION_CODES.TIRAMISU){
                            albumLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*" })}}

                R.id.internetButton ->
                    permissionRequester(Manifest.permission.INTERNET){
                        val connectivityManager=getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                        if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            imgFile=File(externalCacheDir,LocalDateTime.now().toString().replace(":","-")+"jpg").apply { createNewFile() }
                            val editText=EditText(this).apply { hint = "http://..." }
                            AlertDialog.Builder(this)
                                .setTitle("输入图片网址")
                                .setView(editText)
                                .setPositiveButton("确定"){ _, _ ->
                                    Retrofit.Builder().baseUrl("https://www.baidu.com/").build().create(DiaryRetrofit::class.java).getNetPic(editText.text.toString()).enqueue(object :Callback<ResponseBody>{
                                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                            Toast.makeText(this@DiaryActivity,"获取失败",Toast.LENGTH_SHORT).show()}
                                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                            val imgCode = BitmapFactory.decodeStream(response.body()!!.byteStream())
                                            if (imgCode!=null) {
                                                FileOutputStream(imgFile).use {
                                                    imgCode.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                                                setImg(FileProvider.getUriForFile(this@DiaryActivity, "com.example.diaryapp", imgFile).toString()) }
                                            else Toast.makeText(this@DiaryActivity,"获取失败",Toast.LENGTH_SHORT).show()}})}
                                .show()}
                        else Toast.makeText(this@DiaryActivity,"获取失败",Toast.LENGTH_SHORT).show()}}
            true
        }

        binding.diaryTitleEditText.setOnEditorActionListener { v, _, _ ->
            if(v.text.isNullOrEmpty()) v.text = "${diaryViewModel.oneDiary.date}的日记"
            else diaryViewModel.oneDiary.title=v.text.toString()
            true
        }

        binding.diaryDateTextView.setOnClickListener {
            val nowDate = LocalDate.parse("20${diaryViewModel.oneDiary.date}", DateTimeFormatter.ISO_DATE)
            DatePickerDialog(this, null, nowDate.year, nowDate.monthValue-1, nowDate.dayOfMonth).apply {
                setOnDateSetListener { _, i, i2, i3 ->
                    saveDiary()
                    diaryViewModel.oneDiary.date=String.format(Locale.getDefault(), "%02d-%02d-%02d",i%100,i2+1, i3)
                    dismiss()
                    setDiary()
                    }
            }.show()
        }

        cameraLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                setImg(FileProvider.getUriForFile(this@DiaryActivity, "com.example.diaryapp", imgFile).toString())}

        albumLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data!=null) setImg(result.data!!.data.toString())}

        if(diaryViewModel.mode==0) changeToWatchView()
        else changeToEditView()
        setDiary()
    }

    private fun setDiary() {
        diaryViewModel.setOneDiary(diaryViewModel.oneDiary.date)
        diaryViewModel.diaryData=if(File(filesDir, diaryViewModel.oneDiary.date).exists()){
                                    val diaryContent = StringBuilder()
                                    BufferedReader(InputStreamReader(openFileInput(diaryViewModel.oneDiary.date))).use {
                                        it.forEachLine { s -> diaryContent.append(s)}}
                                    diaryContent.toString()}
                                    else ""
        if (LocalDate.now().toString().drop(2)==diaryViewModel.oneDiary.date &&
            (diaryViewModel.oneDiary.city=="未知" || diaryViewModel.oneDiary.weather=="未知")){
            val location: String? =getLocation()
            if (location!=null){
                permissionRequester(Manifest.permission.INTERNET) {
                    val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                    if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                        setCity(location)
                        setWeather(location)}}}}
        setDiaryData()
}

    @SuppressLint("SetTextI18n")
    private fun setDiaryData(){
        diaryViewModel.oneDiary.run {
            binding.diaryCityTextView.text= "城市:${this.city}"
            binding.diaryWeatherTextView.text="天气:${this.weather}"
            binding.diaryDateTextView.text=this.date
            binding.diaryTitleEditText.setText(this.title)}
        if (diaryViewModel.mode==0) setTextView()
        else diaryContentEditText.setText(diaryViewModel.diaryData)}

    @SuppressLint("MissingPermission")
    private fun getLocation(): String? {
        var location: Location?=null
        permissionRequester(Manifest.permission.ACCESS_FINE_LOCATION){
            val locationManager=this.getSystemService(LOCATION_SERVICE) as LocationManager
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F) { p0 -> location = p0 }
            for (provider in locationManager.getProviders(true)) {
                val newLocation = locationManager.getLastKnownLocation(provider)
                if ((newLocation != null) && ((location == null) || (newLocation.accuracy > location!!.accuracy)))
                    location = newLocation }}
        return if(location==null) null
        else String.format("%.2f,%.2f",location!!.longitude,location!!.latitude)
    }

    private fun setCity(location: String){
        Retrofit.Builder().baseUrl("https://geoapi.qweather.com/v2/city/").addConverterFactory(GsonConverterFactory.create()).build().create(
            DiaryRetrofit::class.java).getCity(location).enqueue(object :Callback<CityItem>{
            override fun onResponse(call: Call<CityItem>, response: Response<CityItem>) {
                if (response.body() != null && !response.body()!!.location.isNullOrEmpty() && response.body()!!.location!![0].isNotEmpty())
                    diaryViewModel.oneDiary.city = response.body()!!.location!![0]["name"]!! }
            override fun onFailure(call: Call<CityItem>, t: Throwable) {
                Toast.makeText(this@DiaryActivity,"获取失败",Toast.LENGTH_SHORT).show() }})}

    private fun setWeather(location: String){
        Retrofit.Builder().baseUrl("https://devapi.qweather.com/v7/weather/").addConverterFactory(GsonConverterFactory.create()).build().create(
            DiaryRetrofit::class.java).getWeather(location).enqueue(object :Callback<WeatherItem>{
            override fun onResponse(call: Call<WeatherItem>, response: Response<WeatherItem>) {
                if (response.body()!=null && !response.body()!!.now.isNullOrEmpty()) diaryViewModel.oneDiary.weather=response.body()!!.now!!["text"]!! }
            override fun onFailure(call: Call<WeatherItem>, t: Throwable) {
                Toast.makeText(this@DiaryActivity,"获取失败",Toast.LENGTH_SHORT).show() }})}

    private fun setTextView(){
        binding.diaryInLayout.removeAllViews()
        for (i in diaryViewModel.diaryData.split("<DiaryApp>").withIndex())
            if (i.index % 2==0)
                binding.diaryInLayout.addView(TextView(this).apply { text=i.value })
            else
                binding.diaryInLayout.addView(ImageView(this).apply {
                    Glide.with(this@DiaryActivity).load(getSharedPreferences(diaryViewModel.oneDiary.date,
                        MODE_PRIVATE).getString(i.value,"")?.toUri()).into(this) }) }

    private fun setImg(imgUri:String){
        getSharedPreferences(diaryViewModel.oneDiary.date, MODE_PRIVATE).edit().run {
            putString("img${diaryViewModel.oneDiary.imgCount}", imgUri)
            apply()}
        diaryContentEditText.text.insert(diaryContentEditText.selectionStart,
            "\n<DiaryApp>${"img${diaryViewModel.oneDiary.imgCount}"}<DiaryApp>\n")
        diaryViewModel.oneDiary.imgCount+=1 }

    private fun changeToWatchView(){
        binding.diaryButtonView.menu.clear()
        binding.diaryButtonView.inflateMenu(R.menu.diary_watch_menu)
        binding.diaryButtonView.setOnItemSelectedListener(watchNavigationListener)
        binding.diaryInLayout.removeAllViews()}

    private fun changeToEditView(){
        binding.diaryButtonView.menu.clear()
        binding.diaryButtonView.inflateMenu(R.menu.diary_edit_menu)
        binding.diaryButtonView.setOnItemSelectedListener(editNavigationListener)
        binding.diaryInLayout.removeAllViews()
        binding.diaryInLayout.addView(diaryContentEditText)
    }

    private fun saveDiary(){
        if (diaryViewModel.updateDiary() && diaryViewModel.mode==1) {
            diaryViewModel.diaryData=diaryContentEditText.text.toString()
            BufferedWriter(OutputStreamWriter(openFileOutput(diaryViewModel.oneDiary.date, MODE_PRIVATE))).use {
                it.write(diaryViewModel.diaryData)}}}

    private fun permissionRequester(permission: String, block:()->Unit) {
        if(ContextCompat.checkSelfPermission(this, permission)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(permission),1)}
        else{block()}}

    private fun permissionRequesterPlus(oldPermission: String,newPermission:String,version:Int, block:()->Unit) {
        if(Build.VERSION.SDK_INT < version)
            permissionRequester(oldPermission){block()}
        else permissionRequester(newPermission){block()}}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"申请成功",Toast.LENGTH_SHORT).show()
            else
                Toast.makeText(this,"申请失败",Toast.LENGTH_SHORT).show()}

    override fun finish() {
        saveDiary()
        val intent=Intent()
        setResult(RESULT_OK,intent)
        super.finish()}
}
