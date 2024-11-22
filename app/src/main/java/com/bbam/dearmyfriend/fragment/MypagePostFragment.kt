package com.bbam.dearmyfriend.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.adapter.PostListAdapter
import com.bbam.dearmyfriend.data.LikeResponse
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.data.PostResponse
import com.bbam.dearmyfriend.databinding.FragmentMypagePostBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MypagePostFragment : Fragment() {

    private lateinit var binding: FragmentMypagePostBinding
    private lateinit var adapter: PostListAdapter
    private val itemList: MutableList<PostData> = mutableListOf()

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }
    private val sharedPreferences by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

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

        // 어댑터 초기화
        adapter = PostListAdapter(requireContext(), itemList) { post, isLiked ->
            // 작성한 게시물에서 좋아요 기능이 필요하지 않을 수 있으나, 필요하면 여기에 추가
            toggleLike(post, isLiked)
        }

        // RecyclerView 설정
        binding.recyclerViewMypagePost.adapter = adapter
        binding.recyclerViewMypagePost.layoutManager = LinearLayoutManager(requireContext())

        // 사용자가 작성한 게시물 불러오기
        fetchUserPosts()
    }

    private fun fetchUserPosts() {
        val uid = sharedPreferences.getString("uid", null) ?: return

        retrofitService.getUserPosts(uid).enqueue(object : Callback<PostResponse> {
            override fun onResponse(call: Call<PostResponse>, response: Response<PostResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val posts = response.body()?.posts ?: emptyList()
                    itemList.clear()
                    itemList.addAll(posts)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "게시물을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PostResponse>, t: Throwable) {
                Log.e("MypagePost", "서버 오류: ${t.message}")
                Toast.makeText(requireContext(), "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleLike(post: PostData, isLiked: Boolean) {
        val uid = sharedPreferences.getString("uid", null) ?: return

        retrofitService.toggleLike(post.id, uid).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    post.isLiked = response.body()?.liked ?: isLiked
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "좋아요 처리 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
