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
import com.bbam.dearmyfriend.databinding.FragmentMypagePostBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MypagePostFragment : Fragment() {
    lateinit var binding: FragmentMypagePostBinding

    private lateinit var adapter: PostListAdapter
    private val itemList: MutableList<PostData> = mutableListOf()
    private val db: FirebaseFirestore = Firebase.firestore
    private lateinit var auth: FirebaseAuth


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMypagePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // recyclerview 설정
        adapter = PostListAdapter(requireContext(), itemList) { postData ->

        }

        binding.recyclerViewMypagePost.adapter = adapter
        binding.recyclerViewMypagePost.layoutManager = LinearLayoutManager(requireContext())

        // 게시물 불러오기
        fetchPost()
    }

    private fun fetchPost() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("posts")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { documents ->
                itemList.clear()
                for (document in documents) {
                    val post = document.toObject(PostData::class.java)
                    post.postId = document.id
                    adapter.addPost(post)  // adapter에 추가
                }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "게시물을 불러오지 못했습니다 : ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
