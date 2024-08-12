package com.example.diaryapp.recycleView

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.diaryapp.databinding.DiaryListItemBinding
import com.example.diaryapp.viewModel.DiaryViewModel

class DiaryListAdapter(private val diaryViewModel: DiaryViewModel):RecyclerView.Adapter<DiaryListAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onClick(position: Int)
    }

    var clickListener: OnClickListener? = null
    private var data = ArrayList(diaryViewModel.allDiary)

    inner class ViewHolder(val binding: DiaryListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            this.itemView.setOnClickListener { clickListener?.onClick(adapterPosition) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(DiaryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            diaryListCity.text = "城市:${data[position].city}"
            diaryListDate.text = "日期:${data[position].date}"
            diaryListWeather.text = "天气:${data[position].weather}"
            diaryListTitle.text = data[position].title
        }
    }

    override fun getItemCount() = data.size

    @SuppressLint("NotifyDataSetChanged")
    fun refresh() {
        diaryViewModel.setFilteredTitle()
        data = ArrayList(diaryViewModel.adapterDiary)
        this.notifyDataSetChanged()
    }
}