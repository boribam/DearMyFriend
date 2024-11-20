package com.bbam.dearmyfriend.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.bbam.dearmyfriend.data.LikeResponse
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.data.PostResponse
import com.bbam.dearmyfriend.databinding.FragmentSocialBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SocialFragment : Fragment() {

    lateinit var binding: FragmentSocialBinding
    private var itemList: MutableList<PostData> = mutableListOf()
    private lateinit var adapter: PostListAdapter

    // ActivityResultLauncher 선언
    private lateinit var postWritingLauncher: ActivityResultLauncher<Intent>

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 초기화
        adapter = PostListAdapter(requireContext(), itemList) { post, isLiked ->
            toggleLike(post, isLiked)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 게시물 목록 불러오기
        fetchPost()

        // ActivityResultLauncher 초기화
        postWritingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                fetchPost() // 작성 후 게시물 목록 갱신
            }
        }

        // Floating Action Button 클릭 시 게시물 작성 화면으로 이동
        binding.floatingActionBtn.setOnClickListener {
            startActivity(Intent(requireContext(), PostWritingActivity::class.java))
        }
    }

    private fun fetchPost() {
        binding.progressbar.visibility = View.VISIBLE // 로딩 시작

        retrofitService.getPosts().enqueue(object : Callback<PostResponse> {
            override fun onResponse(p0: Call<PostResponse>, p1: Response<PostResponse>) {
                binding.progressbar.visibility = View.GONE
                if (p1.isSuccessful && p1.body()?.success == true) {
                    itemList.clear()
                    p1.body()?.posts?.let { itemList.addAll(it) }
                    adapter.notifyDataSetChanged()
                } else {
                    Snackbar.make(binding.root, "게시물을 불러오지 못했습니다.", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(p0: Call<PostResponse>, p1: Throwable) {
                binding.progressbar.visibility = View.GONE
                Log.e("fetch post", "서버 오류: ${p1.message}")
                Snackbar.make(binding.root, "서버 오류: ${p1.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleLike(post: PostData, isLiked: Boolean) {
        if (isLiked) {
            retrofitService.likePost(post.id, post.uid).enqueue(object : Callback<LikeResponse> {
                override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                    if (response.isSuccessful) {
                        Snackbar.make(binding.root, "좋아요를 눌렀습니다.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "좋아요 실패", Snackbar.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
                }
            })
        } else {
            retrofitService.unlikePost(post.id, post.uid).enqueue(object : Callback<LikeResponse> {
                override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                    if (response.isSuccessful) {
                        Snackbar.make(binding.root, "좋아요를 취소했습니다.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "좋아요 취소 실패", Snackbar.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
                }
            })
        }
    }
}