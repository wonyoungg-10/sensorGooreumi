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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

                // 파이어베이스에 사용자 정보 업데이트
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
        val startDate = LocalDate.of(2024, 12, 1)
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

        // 파이어베이스 관련

        // 저장 버튼 클릭 시 감정 저장
        binding.save.setOnClickListener {
            // 사용자가 입력한 감정 텍스트 가져오기
            val userEmotion = binding.writeFeeling.text.toString()  // 감정 입력 텍스트 (EditText에서 가져오기)

            if (userEmotion.isNotEmpty()) {
                // 현재 날짜
                // val currentDate = LocalDate.now().toString()  // 예: "2024-11-29"

                // 나중에 응답이 생성될 예정 -> 우선 비워두기
                // val response = ""

                // 감정과 응답을 Map 형태로 준비
                val entry = mapOf(
                    "emotion" to userEmotion
                    // "response" to response
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
                        val entriesRef = FirebaseDatabase.getInstance().getReference("entries")
                        entriesRef.setValue(entry)
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

    override fun onStart() {
        super.onStart()

        // 친밀도 데이터에 대한 실시간 리스너 등록
        userRef.child("actions").addValueEventListener(friendlinessListener)
    }

    override fun onStop() {
        super.onStop()

        // Activity가 중단될 때 리스너 제거
        userRef.child("actions").removeEventListener(friendlinessListener)
    }

    // 리스너 정의
    private val friendlinessListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Firebase에서 데이터 가져오기
            val feedCount = snapshot.child("feeding").getValue(Int::class.java) ?: 0
            val playCount = snapshot.child("playing").getValue(Int::class.java) ?: 0

            // 친밀도 계산 및 ProgressBar 업데이트
            val friendliness = calculateFriendliness(feedCount, playCount)
            binding.count.progress = friendliness.coerceIn(0, 100)
        }

        override fun onCancelled(error: DatabaseError) {
            // 오류 처리
            Toast.makeText(this@BearActivity, "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }
    // 친밀도 계산 함수 -> ProgressBar 업데이트되는 것이 잘 보이도록,, 우선 크게
    private fun calculateFriendliness(feedCount: Int, playCount: Int): Int {
        return (feedCount * 4) + (playCount * 8)
    }
}
