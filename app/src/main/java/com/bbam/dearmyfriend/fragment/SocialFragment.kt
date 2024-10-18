package com.bbam.dearmyfriend.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.PostWritingActivity
import com.bbam.dearmyfriend.adapter.PostListAdapter
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.databinding.FragmentSocialBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SocialFragment : Fragment() {

    lateinit var binding: FragmentSocialBinding
    private var itemList: MutableList<PostData> = mutableListOf()
    private val db = Firebase.firestore
    private lateinit var adapter: PostListAdapter
    private lateinit var auth: FirebaseAuth

    // ActivityResultLauncher 선언
    private lateinit var postWritingLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PostListAdapter(requireContext(), itemList) { post ->
            toggleFavorite(post)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 게시물 목록 불러오기
        fetchPost()

        // ActivityResultLauncher 초기화
        postWritingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                fetchPost()
            }
        }

        // Floating Action Button 클릭 시 게시물 작성 화면으로 이동
        binding.floatingActionBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PostWritingActivity::class.java))
        }
    }

    private fun fetchPost() {
        binding.progressbar.visibility = View.VISIBLE // 로딩 시작

        db.collection("posts")
            .orderBy("dateMillis", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().addOnSuccessListener { documents ->
                itemList.clear()
                for (document in documents) {
                    val post = document.toObject(PostData::class.java)
                    post.postId = document.id

                    adapter.addPost(post)
                }

                binding.progressbar.visibility = View.GONE

            }.addOnFailureListener { e ->
                Snackbar.make(binding.root, "게시물을 불러오지 못했습니다. : ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    private fun toggleFavorite(post: PostData) {
        val userId = auth.currentUser?.uid ?: return

        val favoriteRef =
            db.collection("users").document(userId).collection("favorites").document(post.postId)

        // 즐겨찾기 상태 확인 후 추가/제거
        favoriteRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                favoriteRef.delete().addOnSuccessListener {
                    Snackbar.make(binding.root, "좋아요를 취소하셨습니다.", Snackbar.LENGTH_SHORT).show()
                }
            } else {
                favoriteRef.set(mapOf("postId" to post.postId)).addOnSuccessListener {
                    Snackbar.make(binding.root, "좋아요.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}