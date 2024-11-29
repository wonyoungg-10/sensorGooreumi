package com.example.sensorapp

import android.os.Bundle

import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sensorapp.databinding.ActivityFragmentBinding
import com.example.sensorapp.databinding.ItemRecyclerviewBinding

class MyViewHolder(val binding: ItemRecyclerviewBinding):
    RecyclerView.ViewHolder(binding.root)

class MyAdapter(val datas: List<String>):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun getItemCount(): Int = datas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecyclerView.ViewHolder = MyViewHolder( ItemRecyclerviewBinding.inflate(
        LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val binding = (holder as MyViewHolder).binding
        binding.itemData.text = datas[position]
    }

}

class Fragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = ActivityFragmentBinding.inflate(inflater, container, false)
        val datas = mutableListOf("사과", "포도", "복숭아", "딸기", "키위")

        val layoutManager = LinearLayoutManager(activity)
        binding.diaryText.layoutManager = layoutManager

        val adapter = MyAdapter(datas)
        binding.diaryText.adapter = adapter

        binding.diaryText.addItemDecoration(DividerItemDecoration(activity,LinearLayoutManager.VERTICAL))

        return binding.root


    }
}