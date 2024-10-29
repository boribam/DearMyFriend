package com.bbam.dearmyfriend.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bbam.dearmyfriend.data.RegisterResponse
import com.bbam.dearmyfriend.databinding.ActivitySignupBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import okhttp3.Callback
import retrofit2.Call
import retrofit2.Response
import kotlin.math.log

class SignupActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySignupBinding.inflate(layoutInflater) }

    private var profileImageUri: Uri? = null

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }

    // 이미지 선택 런처
    private val imagePickerLauncher : ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                profileImageUri = uri
                binding.profileImage.setImageURI(uri)  // 사용자가 선택한 이미지로 변경
            }
        }

//    private lateinit var auth: FirebaseAuth
//    private val db = Firebase.firestore
//
//    private var selectedImageUri: Uri? = null
//    private lateinit var getContent: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // 프로필 이미지 선택
        binding.profileImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // 회원가입 버튼
        binding.btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        // 사용자 입력 값 가져오기
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val passwordCheck = binding.etPasswordCheck.text.toString().trim()
        val nickname = binding.etNickname.text.toString().trim()

        // 필수 정보 확인
        if (email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            Snackbar.make(binding.root,"모든 정보를 입력해주세요", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (password != passwordCheck) {
            Snackbar.make(binding.root,"비밀번호가 일치하지 않습니다", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 프로필 이미지가 선택되었는지 확인
        if (profileImageUri != null) {
            uploadProfileImageToFirebase(email, password, nickname, profileImageUri!!)
        } else {
            // 이미지가 없는 경우 기본 URL로 회원가입 처리
            sendUserDataToServer(email, password, nickname, "")
        }
    }

    private fun uploadProfileImageToFirebase(email: String, password: String, nickname: String, uri: Uri) {
        val storageRef = Firebase.storage.reference.child("profile_images/${System.currentTimeMillis()}.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                // 이미지 업로드 성공 시 URL 가져오기
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    sendUserDataToServer(email, password, nickname, imageUrl)
                }
            }.addOnFailureListener {
                Snackbar.make(binding.root, "이미지 업로드에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun sendUserDataToServer(email: String, password: String, nickname: String, profileImageUrl: String) {
        // Dothome 서버로 사용자 정보 전송
        val dataPart = mapOf(
            "user_email" to email,
            "user_password" to password,
            "user_nickname" to nickname,
            "user_profile_image" to profileImageUrl
        )

        retrofitService.registerUser(dataPart).enqueue(object : retrofit2.Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { registerResponse ->
                        // 서버에서 받은 success 여부 확인
                        if (registerResponse.success) {
                            Snackbar.make(binding.root, "회원가입 성공!", Snackbar.LENGTH_INDEFINITE)
                                .setAction("로그인") {
                                    // 로그인 화면으로 이동
                                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                                    finish() // 회원가입 화면 종료
                                }
                                .show()
                        } else {
                            // 회원가입 실패 메시지 표시
                            Snackbar.make(binding.root, registerResponse.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // 회원가입 실패 시 서버에서 받은 에러 메시지 출력
                    val errorBody = response.errorBody()?.string()
                    Log.e("SignupActivity", "회원가입 실패: $errorBody")
                    Snackbar.make(binding.root, "회원가입 실패: $errorBody", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Log.e("SignupActivity", "서버 오류: ${t.message}")
                Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }
}

//        binding.toolbar.setNavigationOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
//        }
//
//        auth = Firebase.auth
//
//        getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//            if (uri != null) {
//                selectedImageUri = uri
//                binding.profile.setImageURI(selectedImageUri)
//            }
//        }
//
//        binding.btnSignUp.setOnClickListener { createAccount() }
//        binding.profile.setOnClickListener { openGallery() }
//    }
//
//    private fun openGallery() {
//        getContent.launch("image/*")
//    }

//    // Firebase Firestore DB에 사용자 정보 저장
//    private fun createAccount() {
//
//        val email = binding.etEmail.text.toString()
//        val pw = binding.etPassword.text.toString()
//        val pwCheck = binding.etPasswordCheck.text.toString()
//        val nickname = binding.etNickname.text.toString()
//
//        var isTrue = true
//
//        if (email.isEmpty()) {
//            Snackbar.make(binding.root, "이메일을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        if (pw.isEmpty()) {
//            Snackbar.make(binding.root, "비밀번호를 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        if (pwCheck.isEmpty()) {
//            Snackbar.make(binding.root, "비밀번호 확인란을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        if (nickname.isEmpty()) {
//            Snackbar.make(binding.root, "닉네임을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        if (!pw.equals(pwCheck)) {
//            Snackbar.make(binding.root, "비밀번호가 일치하지 않습니다.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        if (pw.length < 6) {
//            Snackbar.make(binding.root, "비밀번호는 6자리 이상 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//            isTrue = false
//        }
//
//        // 중복 닉네임 체크
//        if (isTrue) {
//            db.collection("users")
//                .whereEqualTo("nickname", nickname)
//                .get()
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful && task.result != null) {
//                        if (task.result.isEmpty) {
//                            // 중복된 닉네임이 없으면 회원가입 진행
//                            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener(this) { authTask ->
//                                if (authTask.isSuccessful) {
//                                    saveUserInfo(auth.currentUser?.uid, nickname, email)
//                                } else {
//                                    Snackbar.make(binding.root, "중복된 이메일입니다. 다른 이메일을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
//                                }
//                            }
//                        } else {
//                            // 중복된 닉네임이 있을 경우
//                            Snackbar.make(binding.root, "이미 사용 중인 닉네임입니다.", Snackbar.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Snackbar.make(binding.root, "닉네임 확인 실패", Snackbar.LENGTH_SHORT).show()
//                    }
//                }
//        }
//    }

//    private fun saveUserInfo(userId: String?, nickname: String, email: String) {
//        userId?.let { uid ->
//            val userInfo = hashMapOf(
//                "uid" to uid,
//                "nickname" to nickname,
//                "email" to email
//            )
//
//            selectedImageUri?.let { uri ->
//                val storageRef = Firebase.storage.reference
//                val profileImageRef = storageRef.child("profile_images/${uid}.jpg")
//
//                profileImageRef.putFile(uri).addOnSuccessListener {
//                    profileImageRef.downloadUrl.addOnSuccessListener { imageUrl ->
//                        userInfo["profileImage"] = imageUrl.toString()
//                        db.collection("users").document(uid)
//                            .set(userInfo)
//                            .addOnSuccessListener {
//                                Snackbar.make(binding.root, "회원가입이 완료되었습니다", Snackbar.LENGTH_SHORT)
//                                    .show()
//                                val intent = Intent(this, MainActivity::class.java)
//                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                                startActivity(intent)
//                                finish()
//                            }.addOnFailureListener { e ->
//                                Log.e("signup failed", "${e.message}")
//                            }
//                    }
//                }.addOnFailureListener { e ->
//                    Log.e("image upload failed", "${e.message}")
//                }
//            } ?: run {
//                // 이미지가 선택되지 않은 경우 사용자 정보를 저장
//                db.collection("users").document(uid)
//                    .set(userInfo)
//                    .addOnSuccessListener {
//                        Snackbar.make(binding.root, "회원가입이 완료되었습니다.", Snackbar.LENGTH_SHORT).show()
//                        val intent = Intent(this, MainActivity::class.java)
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
//                        startActivity(intent)
//                        finish()
//                    }.addOnFailureListener { e ->
//                        Log.e("signup failed", "${e.message}")
//                    }
//            }
//        }
//    }
