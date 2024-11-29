package com.example.sensorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.sensorapp.databinding.ActivityDiaryBinding

class DiaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val binding = ActivityDiaryBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 돌아가기 버튼을 누르면 -> 메인 화면 BearActivity로 이동
        binding.toBear2.setOnClickListener {
            finish() // UserActivity 종료
        }

    }
}

