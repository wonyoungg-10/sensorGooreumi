package com.example.sensorapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.sensorapp.databinding.ActivityBearBinding
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

                // 파이어베이스 사용자 정보 업데이트
                updateUserInDatabase(userName, userWake, userSleep)
            }
        }

    // Firebase Realtime Database 인스턴스 가져오기
    private val database = FirebaseDatabase.getInstance()
    private val userRef = database.getReference("user")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityBearBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 우리가 함께한지 +N일
        val startDate = LocalDate.of(2024, 11, 15)
        val today = LocalDate.now()
        val dayDifference = ChronoUnit.DAYS.between(startDate, today) + 1

        //textView에 표시 (id:days)
        binding.days.text = "+${dayDifference}일"


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

        // 별 모양 FloatingActionButton 클릭하면 이전의 감정 기록을 볼 수 있는 페이지로 이동
        binding.diary.setOnClickListener {
            val intent = Intent(this, DiaryActivity::class.java)
            startActivity(intent)
        }


        // 파이어베이스 관련

// 저장 버튼 클릭 시 감정 저장
        binding.save.setOnClickListener {
            // 사용자가 입력한 감정 텍스트 가져오기
            val userEmotion = binding.writeFeeling.text.toString()  // 감정 입력 텍스트 (EditText에서 가져오기)

            if (userEmotion.isNotEmpty()) {
                // 현재 날짜
                val currentDate = LocalDate.now().toString()  // 예: "2024-11-29"

                // 감정에 맞는 응답 생성 (이 예시에서는 간단한 응답만 생성)
                val response = generateResponse(userEmotion)

                // 감정과 응답을 Map 형태로 준비
                val entry = mapOf(
                    "emotion" to userEmotion,
                    "response" to response
                )

                // Firebase에 해당 날짜의 감정 기록 삽입
                val userId = "userId"  // 사용자 고유 ID (앱에서 관리)
                val userRef = FirebaseDatabase.getInstance().getReference("user").child(userId)

                // 사용자 정보는 updateChildren으로 갱신하고, entries는 별도로 추가
                val userData = mapOf(
                    "name" to binding.name.text.toString(),
                    "wakeTime" to binding.wakeTime.text.toString(),
                    "sleepTime" to binding.sleepTime.text.toString()
                )

                // 사용자 정보 업데이트
                userRef.updateChildren(userData)
                    .addOnSuccessListener {
                        // entries 경로에 감정 데이터 추가
                        val entriesRef =
                            FirebaseDatabase.getInstance().getReference("entries").child(userId)
                        entriesRef.child(currentDate).setValue(entry)
                            .addOnSuccessListener {
                                // 저장 성공 후 처리
                                Toast.makeText(this, "감정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                // 저장 실패 후 처리
                                Toast.makeText(this, "감정 저장 실패", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener {
                        // 사용자 정보 업데이트 실패 시 처리
                        Toast.makeText(this, "사용자 정보 업데이트 실패", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // 감정 입력란이 비어있는 경우
                Toast.makeText(this, "감정을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Firebase에서 사용자 정보를 업데이트하는 함수
    private fun updateUserInDatabase(userName: String, wakeTime: String, sleepTime: String) {
        val userId = "userId"  // 사용자 고유 ID (앱에서 관리)
        val userRef = FirebaseDatabase.getInstance().getReference("user").child(userId)

        // 사용자 정보 업데이트
        val updatedUserData = mapOf(
            "name" to userName,
            "wakeTime" to wakeTime,
            "sleepTime" to sleepTime
        )

        userRef.updateChildren(updatedUserData)
            .addOnSuccessListener {
                // 이름 업데이트 성공 시 처리
                Toast.makeText(this, "사용자 정보가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // 이름 업데이트 실패 시 처리
                Toast.makeText(this, "사용자 정보 업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }

    // 감정에 맞는 응답을 생성하는 함수 (단순 예시)
    private fun generateResponse(emotion: String): String {
        return when (emotion) {
            "행복하다" -> "오늘은 행복한 하루를 보냈구나! 즐거웠다니 나도 기분이 좋다~"
            "지쳤어요" -> "오늘 하루 힘들었지... 푹 쉬어. 내일은 행복하길!"
            "슬프다" -> "슬픈 일이 있었구나... 괜찮아, 내가 항상 옆에 있어."
            else -> "오늘 하루도 잘 보냈네! 잘했어!"
        }
    }
}

