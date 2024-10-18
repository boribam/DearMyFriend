package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bbam.dearmyfriend.G
import com.bbam.dearmyfriend.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = Firebase.auth

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // 사용자가 로그인된 상태일 경우, 메인 화면으로 이동
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener{ clickLogin() }

        binding.signUp.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun clickLogin() {
        val email = binding.etEmail.text.toString()
        val pw = binding.etPassword.text.toString()

        if (email.isEmpty()) {
            Snackbar.make(binding.root, "이메일을 입력해주세요", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (pw.isEmpty()) {
            Snackbar.make(binding.root, "비밀번호를 입력해주세요", Snackbar.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                G.userId = auth.currentUser?.uid
                G.userEmail = auth.currentUser?.email

                val intent = Intent(this, MainActivity::class.java).putExtra("userId", auth.currentUser?.uid)
                startActivity(intent)

                finish() // 로그인 후에는 로그인 화면을 종료한다.

            } else {
                Log.e("login", "Login failed: ${task.exception?.message}")
                Snackbar.make(binding.root, "이메일과 비밀번호를 다시 한번 확인해주세요", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}