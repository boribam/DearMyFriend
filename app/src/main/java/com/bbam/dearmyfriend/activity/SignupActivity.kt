package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bbam.dearmyfriend.MainActivity
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.databinding.ActivitySignupBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class SignupActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySignupBinding.inflate(layoutInflater) }

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    private var selectedImageUri: Uri? = null
    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        auth = Firebase.auth

        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.profile.setImageURI(selectedImageUri)
            }
        }

        binding.btnSignUp.setOnClickListener { createAccount() }
        binding.profile.setOnClickListener { openGallery() }
    }

    private fun openGallery() {
        getContent.launch("image/*")
    }

    // Firebase Firestore DB에 사용자 정보 저장
    private fun createAccount() {

        val email = binding.etEmail.text.toString()
        val pw = binding.etPassword.text.toString()
        val pwCheck = binding.etPasswordCheck.text.toString()
        val nickname = binding.etNickname.text.toString()

        var isTrue = true

        if (email.isEmpty()) {
            Snackbar.make(binding.root, "이메일을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        if (pw.isEmpty()) {
            Snackbar.make(binding.root, "비밀번호를 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        if (pwCheck.isEmpty()) {
            Snackbar.make(binding.root, "비밀번호 확인란을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        if (nickname.isEmpty()) {
            Snackbar.make(binding.root, "닉네임을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        if (!pw.equals(pwCheck)) {
            Snackbar.make(binding.root, "비밀번호가 일치하지 않습니다.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        if (pw.length < 6) {
            Snackbar.make(binding.root, "비밀번호는 6자리 이상 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            isTrue = false
        }

        // 중복 닉네임 체크
        if (isTrue) {
            db.collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        if (task.result.isEmpty) {
                            // 중복된 닉네임이 없으면 회원가입 진행
                            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(this) { authTask ->
                                if (authTask.isSuccessful) {
                                    saveUserInfo(auth.currentUser?.uid, nickname, email)
                                } else {
                                    Snackbar.make(binding.root, "중복된 이메일입니다. 다른 이메일을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // 중복된 닉네임이 있을 경우
                            Snackbar.make(binding.root, "이미 사용 중인 닉네임입니다.", Snackbar.LENGTH_SHORT).show()
                        }
                    } else {
                        Snackbar.make(binding.root, "닉네임 확인 실패", Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserInfo(userId: String?, nickname: String, email: String) {
        userId?.let { uid ->
            val userInfo = hashMapOf(
                "uid" to uid,
                "nickname" to nickname,
                "email" to email
            )

            selectedImageUri?.let { uri ->
                val storageRef = Firebase.storage.reference
                val profileImageRef = storageRef.child("profile_images/${uid}.jpg")

                profileImageRef.putFile(uri).addOnSuccessListener {
                    profileImageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        userInfo["profileImage"] = imageUrl.toString()
                        db.collection("users").document(uid)
                            .set(userInfo)
                            .addOnSuccessListener {
                                Snackbar.make(binding.root, "회원가입이 완료되었습니다", Snackbar.LENGTH_SHORT)
                                    .show()
                                val intent = Intent(this, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                finish()
                            }.addOnFailureListener { e ->
                                Log.e("signup failed", "${e.message}")
                            }
                    }
                }.addOnFailureListener { e ->
                    Log.e("image upload failed", "${e.message}")
                }
            } ?: run {
                // 이미지가 선택되지 않은 경우 사용자 정보를 저장
                db.collection("users").document(uid)
                    .set(userInfo)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "회원가입이 완료되었습니다.", Snackbar.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener { e ->
                        Log.e("signup failed", "${e.message}")
                    }
            }
        }
    }
}