package com.bbam.dearmyfriend.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.adapter.PostListAdapter
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.databinding.FragmentMypageFavoriteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MypageFavoriteFragment : Fragment() {

    lateinit var binding: FragmentMypageFavoriteBinding

    private lateinit var adapter: PostListAdapter
    private val itemList: MutableList<PostData> = mutableListOf()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMypageFavoriteBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        adapter = PostListAdapter(requireContext(), itemList) { postData ->
            // 게시물 클릭 시 처리 (필요한 경우)
        }

        binding.recyclerViewMypageFav.adapter = adapter
        binding.recyclerViewMypageFav.layoutManager = LinearLayoutManager(requireContext())

        // 좋아요한 게시물 불러오기
        fetchLikedPosts()
    }
    private fun fetchLikedPosts() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("favorites")
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear() // 기존 목록 초기화
                for (document in documents) {
                    val postId = document.getString("postId") ?: continue
                    fetchPostDetails(postId) // 각 게시물 세부정보 가져오기
                }
            }
            .addOnFailureListener { e ->
                // 에러 처리 (예: Toast 메시지 표시)
            }
    }

    private fun fetchPostDetails(postId: String) {
        db.collection("posts").document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val post = document.toObject(PostData::class.java)
                    post?.postId = document.id // 게시물 ID 설정
                    if (post != null) {
                        adapter.addPost(post) // 어댑터에 추가
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "실패 : ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    fun removePost(postId: String) {
        val position = itemList.indexOfFirst { it.postId == postId }
        if (position != -1) {
            itemList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }
}