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
import com.bbam.dearmyfriend.data.PostData
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

        // RecyclerView 설정
//        adapter = PostListAdapter(requireContext(), itemList) { postData ->
//            // 클릭 이벤트 필요 시 추가
//        }

        binding.recyclerViewMypagePost.adapter = adapter
        binding.recyclerViewMypagePost.layoutManager = LinearLayoutManager(requireContext())

        // 사용자가 작성한 게시물 불러오기
        fetchUserPosts()
    }

    private fun fetchUserPosts() {
        val uid = sharedPreferences.getString("uid", null) ?: return

        retrofitService.getUserPosts(uid).enqueue(object : Callback<List<PostData>> {
            override fun onResponse(call: Call<List<PostData>>, response: Response<List<PostData>>) {
                if (response.isSuccessful && response.body() != null) {
                    itemList.clear()
                    itemList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(requireContext(), "게시물을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<PostData>>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
