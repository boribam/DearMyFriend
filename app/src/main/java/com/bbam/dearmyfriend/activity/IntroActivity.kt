package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {

    private val binding by lazy { ActivityIntroBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 1.5초 동안 인트로 화면 유지 후 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false)

            if (isLoggedIn) {
                // 로그인 상태 유지 시 MainActivity로 이동
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // 로그인 필요 시 LoginActivity로 이동
                startActivity(Intent(this, LoginActivity::class.java))
            }
            // IntroActivity 종료
            finish()
        }, 1500) // 1.5초 대기
    }
}