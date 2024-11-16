package com.example.sensorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sensorapp.databinding.ActivityUserBinding

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("wakeTime")) {
            val userNameV = intent.getStringExtra("name")
            val userWakeV = intent.getStringExtra("wakeTime")
            val userSleepV = intent.getStringExtra("sleepTime")

            binding.userName.setText(userNameV)
            binding.userWake.setText(userWakeV)
            binding.userSleep.setText(userSleepV)
        }

        // 돌아가기 버튼 누르면 -> 사용자의 정보를 가지고 메인 화면으로 이동 (BearActivity)
        binding.toBear.setOnClickListener {
            val userName = binding.userName.text.toString()
            val userWake = binding.userWake.text.toString()
            val userSleep = binding.userSleep.text.toString()

            // 결과 데이터를 설정하여 BearActivity로 전달
            val resultIntent = Intent().apply {
                putExtra("userName", userName)
                putExtra("userWake", userWake)
                putExtra("userSleep", userSleep)
            }

            // 결과 반환 (BearActivity로 돌아감)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // UserActivity 종료
        }
    }
}