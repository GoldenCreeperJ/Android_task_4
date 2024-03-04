package com.example.diaryapp.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.diaryapp.databinding.ActivityMainBinding
import com.example.diaryapp.recycleView.DiaryItemMargin
import com.example.diaryapp.recycleView.DiaryListAdapter
import com.example.diaryapp.viewModel.DiaryApplication
import com.example.diaryapp.viewModel.DiaryViewModel
import java.time.LocalDate

class MainActivity : AppCompatActivity(),DiaryListAdapter.OnClickListener {
    private lateinit var binding:ActivityMainBinding
    private lateinit var diaryViewModel: DiaryViewModel
    private lateinit var diaryListAdapter: DiaryListAdapter
    private lateinit var pagerLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        diaryViewModel=DiaryApplication.diaryViewModel
        setContentView(binding.root)

        binding.diaryList.layoutManager=LinearLayoutManager(this)
            .apply { orientation=LinearLayoutManager.VERTICAL }
        binding.diaryList.adapter=DiaryListAdapter(diaryViewModel)
            .apply { clickListener=this@MainActivity }
        binding.diaryList.addItemDecoration(DiaryItemMargin())
        diaryListAdapter=binding.diaryList.adapter as DiaryListAdapter

        binding.listFilterEditText.setOnEditorActionListener { v, _, _ ->
            diaryViewModel.filterWords=v.text.toString()
            diaryListAdapter.refresh()
            true}

        binding.floatingActionButton.setOnClickListener{
            diaryViewModel.mode=1
            diaryViewModel.oneDiary.date=LocalDate.now().toString().drop(2)
            pagerLauncher.launch(Intent(this, DiaryActivity::class.java))}

        pagerLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) diaryListAdapter.refresh()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(position: Int) {
        diaryViewModel.mode=0
        diaryViewModel.oneDiary.date=diaryViewModel.adapterDiary[position].date
        pagerLauncher.launch(Intent(this, DiaryActivity::class.java))}
}