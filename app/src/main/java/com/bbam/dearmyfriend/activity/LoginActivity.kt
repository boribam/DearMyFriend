package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

        // 로그인 상태 확인
//        checkLoginStatus()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 필수 정보 확인
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(binding.root, "모든 정보를 입력해주세요", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUser(email, password)
        }
    }

    private fun loginUser(email: String, password: String) {
        retrofitService.loginUser(email, password).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(p0: Call<LoginResponse>, p1: Response<LoginResponse>) {
                if (p1.isSuccessful) {
                    val loginResponse = p1.body()
                    if (loginResponse?.success == true) {
                        Snackbar.make(binding.root, "로그인 성공!", Snackbar.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        // 로그인 실패 시 메세지 표시
                        Snackbar.make(binding.root, loginResponse?.message ?: "로그인 실패", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("LoginActivity", "서버 응답 실패: ${p1.errorBody()?.string()}")
                    Snackbar.make(binding.root, "서버 응답 실패", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(p0: Call<LoginResponse>, p1: Throwable) {
                Log.e("LoginActivity", "서버 오류: ${p1.message}")
                Snackbar.make(binding.root, "서버 오류: ${p1.message}", Snackbar.LENGTH_SHORT).show()
            }

        })
    }

//    private fun checkLoginStatus() {
//        retrofitService.checkSession().enqueue(object : Callback<SessionResponse> {
//            override fun onResponse(p0: Call<SessionResponse>, p1: Response<SessionResponse>) {
//                if (p1.isSuccessful) {
//                    val sessionResponse = p1.body()
//                    if (sessionResponse?.logged_in == true) {
//                        // 이미 로그인 된 상태라면 메인 화면으로 이동
//                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
//                        finish()
//                    }
//                } else {
//                    Log.e("LoginActivity", "세션 확인 실패: ${p1.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(p0: Call<SessionResponse>, p1: Throwable) {
//                Log.e("LoginActivity", "서버 오류: ${p1.message}")
//            }
//        })
//    }
}