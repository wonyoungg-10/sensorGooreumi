package com.example.sensorapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.sensorapp.databinding.ActivityBearBinding

class BearActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBearBinding

    private val requestLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // UserActivity에서 반환된 데이터 가져오기
                val data = result.data
                val userName = data?.getStringExtra("userName") ?: "O O O"
                val userWake = data?.getStringExtra("userWake") ?: "00:00"
                val userSleep = data?.getStringExtra("userSleep") ?: "00:00"

                // 값 업데이트
                binding.name.text = userName
                binding.wakeTime.text = userWake
                binding.sleepTime.text = userSleep
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBearBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // 곰돌이 이미지 클릭하면 UserActivity로 이동
        binding.userSetting.setOnClickListener {
            val intent = Intent(this, UserActivity::class.java)

            if (binding.sleepTime.text.toString() != "") {
                val nameValue = binding.name.text.toString()
                val wakeTimeValue = binding.wakeTime.text.toString()
                val sleepTimeValue = binding.sleepTime.text.toString()

                intent.putExtra("name", nameValue)
                intent.putExtra("wakeTime", wakeTimeValue)
                intent.putExtra("sleepTime", sleepTimeValue)
            }
            requestLauncher.launch(intent)
        }

    }

}