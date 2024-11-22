package com.bbam.dearmyfriend.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.adapter.PostListAdapter
import com.bbam.dearmyfriend.data.LikeResponse
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.data.LikedPostsResponse
import com.bbam.dearmyfriend.databinding.FragmentMypageFavoriteBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MypageFavoriteFragment : Fragment() {

    private lateinit var binding: FragmentMypageFavoriteBinding
    private lateinit var adapter: PostListAdapter
    private val itemList: MutableList<PostData> = mutableListOf()

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }
    private val sharedPreferences by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageFavoriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 어댑터 초기화
        adapter = PostListAdapter(requireContext(), itemList) { post, isLiked ->
            toggleLike(post, isLiked) // 좋아요 토글
        }

        // RecyclerView 설정
        binding.recyclerViewMypageFav.adapter = adapter
        binding.recyclerViewMypageFav.layoutManager = LinearLayoutManager(requireContext())

        // 좋아요한 게시물 불러오기
        fetchLikedPosts()
    }

    private fun fetchLikedPosts() {
        val uid = sharedPreferences.getString("uid", null) ?: return

        retrofitService.getLikedPosts(uid).enqueue(object : Callback<LikedPostsResponse> {
            override fun onResponse(call: Call<LikedPostsResponse>, response: Response<LikedPostsResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val likedPosts = response.body()?.likedPosts ?: emptyList()
                    itemList.clear()
                    itemList.addAll(likedPosts)
                    adapter.notifyDataSetChanged() // 어댑터에 데이터 갱신
                } else {
                    Toast.makeText(requireContext(), "좋아요한 게시물을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LikedPostsResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun toggleLike(post: PostData, isLiked: Boolean) {
        val uid = sharedPreferences.getString("uid", null) ?: return

        retrofitService.toggleLike(post.id, uid).enqueue(object : Callback<LikeResponse> {
            override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    post.isLiked = response.body()?.liked ?: false
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
