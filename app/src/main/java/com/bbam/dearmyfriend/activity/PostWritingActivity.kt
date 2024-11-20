package com.bbam.dearmyfriend.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.adapter.PostPictureAdapter
import com.bbam.dearmyfriend.data.PostResponse
import com.bbam.dearmyfriend.databinding.ActivityPostWritingBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostWritingActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPostWritingBinding.inflate(layoutInflater) }
    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }
    private val sharedPreferences by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    private val uriList = ArrayList<Uri>()
    private val maxNumber = 5
    private lateinit var adapter: PostPictureAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        adapter = PostPictureAdapter(this, uriList)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.imageArea.setOnClickListener {
            if (uriList.size == maxNumber) {
                Toast.makeText(this, "이미지는 최대 ${maxNumber}장까지 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            registerForActivityResult.launch(intent)
        }

        binding.btnUploadPost.setOnClickListener {
            uploadPost()
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

    private fun uploadPost() {
        val uid = sharedPreferences.getString("uid", null) ?: run {
            Snackbar.make(binding.root, "로그인이 필요합니다.", Snackbar.LENGTH_SHORT).show()
            return
        }

        val content = binding.etContent.text.toString()
        if (content.isBlank()) {
            Snackbar.make(binding.root, "내용을 입력해주세요.", Snackbar.LENGTH_SHORT).show()
            return
        }

        binding.progressbar.visibility = View.VISIBLE

        if (uriList.isEmpty()) {
            savePostToDothome(uid, content, listOf())
        } else {
            uploadImagesToFirebase(uid, content)
        }
    }

    private fun uploadImagesToFirebase(uid: String, content: String) {
        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        for (uri in uriList) {
            val storageRef = Firebase.storage.reference.child("images/$uid/${System.currentTimeMillis()}.png")
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    imageUrls.add(downloadUri.toString())
                    uploadCount++

                    if (uploadCount == uriList.size) {
                        savePostToDothome(uid, content, imageUrls)
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("PostWritingActivity", "Image upload failed: ${e.message}")
                Snackbar.make(binding.root, "이미지 업로드 실패: ${e.message}", Snackbar.LENGTH_SHORT).show()
                binding.progressbar.visibility = View.GONE
            }
        }
    }

    private fun savePostToDothome(uid: String, content: String, imageUrls: List<String>) {
        val dateMillis = System.currentTimeMillis()
        val dateFormatted = formatDate(dateMillis)

        // Gson을 사용하여 이미지 URL 리스트를 JSON 문자열로 변환
        val gson = com.google.gson.Gson()
        val imageUrlsJson = gson.toJson(imageUrls)

        retrofitService.uploadPost(uid, content, dateMillis, dateFormatted, imageUrlsJson)
            .enqueue(object : Callback<PostResponse> {
                override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                    binding.progressbar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@PostWritingActivity, "게시물 작성 성공", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("PostWritingActivity", "Server error: ${response.errorBody()?.string()}")
                        Snackbar.make(binding.root, "게시물 저장 실패: ${response.body()?.message}", Snackbar.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                    binding.progressbar.visibility = View.GONE
                    Log.e("PostWritingActivity", "Request failed: ${t.message}")
                    Snackbar.make(binding.root, "네트워크 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
                }
            })
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    @SuppressLint("NotifyDataSetChanged")
    private val registerForActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val clipData = result.data?.clipData
            if (clipData != null) {
                val selectableCount = maxNumber - uriList.size
                if (clipData.itemCount > selectableCount) {
                    Toast.makeText(this, "이미지는 최대 ${selectableCount}장까지 첨부할 수 있습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                }
            } else {
                result.data?.data?.let { uriList.add(it) }
            }
            adapter.notifyDataSetChanged()
            printCount()
        }
    }

    private fun printCount() {
        binding.countArea.text = "${uriList.size}/$maxNumber"
    }
}
