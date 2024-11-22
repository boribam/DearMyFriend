package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bbam.dearmyfriend.data.LoginResponse
import com.bbam.dearmyfriend.data.SessionResponse
import com.bbam.dearmyfriend.databinding.ActivityLoginBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 필수 정보 확인
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "모든 정보를 입력해주세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 이메일 형식 확인
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(binding.root, "유효한 이메일을 입력해주세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ProgressBar 표시
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false // 버튼 비활성화

            // 로그인 처리
            loginUser(email, password)
        }

        binding.signUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun loginUser(email: String, password: String) {
        retrofitService.loginUser(email, password).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(p0: Call<LoginResponse>, p1: Response<LoginResponse>) {
                // ProgressBar 숨김
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true // 버튼 활성화

                if (p1.isSuccessful) {
                    val loginResponse = p1.body()
                    if (loginResponse?.success == true) {

                        // SharedPreferences에 이메일과 UID 저장
                        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putString("email", email)
                            .putString("uid", loginResponse.uid)
                            .putBoolean("is_logged_in", true) // 로그인 상태 저장
                            .apply()

                        // 저장 확인 로그
                        val savedUid = sharedPreferences.getString("uid", null)
                        Log.d("LoginActivity", "Saved UID in SharedPreferences: $savedUid")

                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        // 로그인 실패 시 메세지 표시
                        Log.e("LoginActivity", "Login Failed: ${loginResponse?.message}")
                        Snackbar.make(binding.root, loginResponse?.message ?: "로그인 실패", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    // 서버 응답 실패 처리
                    val errorBody = p1.errorBody()?.string()
                    Log.e("LoginActivity", "Server Error: $errorBody")
                    Snackbar.make(binding.root, "서버 오류: $errorBody", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(p0: Call<LoginResponse>, p1: Throwable) {
                // ProgressBar 숨김
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true // 버튼 활성화

                // 서버 요청 자체가 실패했을 때 처리
                Log.e("LoginActivity", "Request Failed: ${p1.message}")
                Snackbar.make(binding.root, "서버 요청 실패: ${p1.message}", Snackbar.LENGTH_SHORT).show()
            }

        })
    }
}