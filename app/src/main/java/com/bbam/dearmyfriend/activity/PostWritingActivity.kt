package com.bbam.dearmyfriend.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.adapter.PostPictureAdapter
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.databinding.ActivityPostWritingBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostWritingActivity: AppCompatActivity() {

    private val binding by lazy { ActivityPostWritingBinding.inflate(layoutInflater) }

    private lateinit var auth: FirebaseAuth

    private lateinit var uri: Uri
    private val uriList = ArrayList<Uri>()
    private val maxNumber = 5
    lateinit var adapter: PostPictureAdapter

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(/* savedInstanceState = */ savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance() // FirebaseAuth 초기화

        adapter = PostPictureAdapter(this, uriList)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // ImageView를 클릭할 경우
        // 선택 가능한 이미지의 최대 개수를 초과하지 않았을 경우에만 앨범 호출하기
        binding.imageArea.setOnClickListener {
            if (uriList.count() == maxNumber) {
                Toast.makeText(this, "이미지는 최대 ${maxNumber}장까지 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            registerForActivityResult.launch(intent)
        }

        binding.btnUploadPost.setOnClickListener {
            uploadPost() // 게시물 업로드 메서드 호출
        }

        adapter.setItemClickListener(object : PostPictureAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                uriList.removeAt(position)
                adapter.notifyDataSetChanged()
                printCount()
            }
        })

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun uploadPost() {

        val currentUserUid = auth.currentUser!!.uid

        val currentDateMillis = System.currentTimeMillis()

        db.collection("users").document(currentUserUid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val nickname = document.getString("nickname")
                val profileImageUri = document.getString("profileImage")

//                val currentDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

                val postData = PostData(
                    content = binding.etContent.text.toString(),
                    uid = currentUserUid,
                    userId = auth.currentUser?.email,
                    nickname = nickname,
                    dateMillis = currentDateMillis,
                    dateFormatted = formatDate(currentDateMillis),
                    profileImageUri = profileImageUri
                )

                binding.progressbar.visibility = View.VISIBLE

                // Firebase Storage에 이미지 업로드
                //        val imageUriList = uriList // 선택한 이미지 URI 목록
                val imageUrls = mutableListOf<String>()
                var uploadCount = 0

                for (uri in uriList) {
                    val storageRef =
                        Firebase.storage.reference.child("images/${auth.uid}/${System.currentTimeMillis()}.png")
                    storageRef.putFile(uri).addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            imageUrls.add(downloadUri.toString()) // 업로드된 이미지 URL 추가
                            uploadCount++

                            if (uploadCount == uriList.size) {
                                postData.imageUri = ArrayList(imageUrls)
                                savePostToFirestore(postData)
                            }
                        }
                    }.addOnFailureListener {
                        Snackbar.make(binding.root, "이미지 업로드 실패", Snackbar.LENGTH_SHORT).show()
                    }
                }
            } else {
                Snackbar.make(binding.root, "사용자 정보를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Snackbar.make(binding.root, "닉네임 가져오기 실패: ${e.message}", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun savePostToFirestore(postData: PostData) {
        val currentDateMillis = System.currentTimeMillis()  // 현재 시간을 밀리초로 저장
        postData.dateMillis = currentDateMillis
        postData.dateFormatted = formatDate(currentDateMillis)

        db.collection("posts").add(postData)
            .addOnSuccessListener {
                setResult(RESULT_OK)
                Snackbar.make(binding.root, "게시글 작성 완료", Snackbar.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
                finish() // 활동 종료 후 이전 화면으로 돌아감
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "게시글 작성 실패", Snackbar.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private val registerForActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                val clipData = result.data?.clipData
                if (clipData != null) {  // 이미지를 여러 장 선택할 경우
                    val clipDataSize = clipData.itemCount
                    val selectableCount = maxNumber - uriList.count()
                    if (clipDataSize > selectableCount) { // 최대 선택 가능한 개수를 초과해서 선택한 경우
                        Toast.makeText(this, "이미지는 최대 ${selectableCount}장까지 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        // 선택 가능한 경우 ArrayList에 가져온 uri를 넣어줌
                        for (i in 0 until clipDataSize) {
                            uriList.add(clipData.getItemAt(i).uri)
                        }
                    }

                } else {
                    // 이미지를 한 장만 선택할 경우 null 이 올 수 있음
                    val uri = result?.data?.data
                    if (uri != null) {
                        uriList.add(uri)
                    }
                }
                // notifyDataSetChanged()를 호출하여 Adapter에게 값이 변경되었음을 알림
                adapter.notifyDataSetChanged()
                printCount()
            }
        }
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    private fun printCount() {
        val text = "${uriList.count()}/${maxNumber}"
        binding.countArea.text = text
    }
}