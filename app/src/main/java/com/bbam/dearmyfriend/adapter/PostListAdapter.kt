package com.bbam.dearmyfriend.adapter

import ImageSliderAdapter
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.PostCommentActivity
import com.bbam.dearmyfriend.data.LikeResponse
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.databinding.RecyclerItemPostListBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class PostListAdapter(
    private val context: Context,
    private val itemList: MutableList<PostData>,
    private val onLikeToggle: (PostData, Boolean) -> Unit // 서버를 통한 좋아요 관리 콜백
) : RecyclerView.Adapter<PostListAdapter.VH>() {

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }

    inner class VH(val binding: RecyclerItemPostListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RecyclerItemPostListBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val postData = itemList[position]

        // 닉네임 설정
        holder.binding.tvNickname.text = postData.nickname ?: "익명"

        // 게시물 내용 설정
        holder.binding.tvContent.text = postData.content

        // 날짜 설정
        holder.binding.tvDate.text = formatDate(postData.dateMillis)

        // 프로필 사진 로드
        Glide.with(context)
            .load(postData.profileImage)
            .placeholder(R.drawable.user) // 기본 프로필 이미지
            .into(holder.binding.ivProfile)

        // 게시물 이미지 슬라이더 설정 (ViewPager2)
        if (!postData.imageUrls.isNullOrEmpty()) {
            val imageAdapter = ImageSliderAdapter(postData.imageUrls)
            holder.binding.viewPager.adapter = imageAdapter
        }

        // 댓글 아이콘 클릭 이벤트
        holder.binding.ivComment.setOnClickListener {
            val intent = Intent(context, PostCommentActivity::class.java).apply {
                putExtra("postId", postData.id) // 서버에서 게시물 ID로 댓글 불러오기
                putExtra("nickname", postData.nickname)
            }
            context.startActivity(intent)
        }

        // 좋아요 버튼 상태 초기화
        holder.binding.tbFavorite.isChecked = postData.isLiked

        // 좋아요 버튼 클릭 이벤트
        holder.binding.tbFavorite.setOnCheckedChangeListener { _, isChecked ->
            postData.isLiked = isChecked
            handleLikeToggle(postData, isChecked, holder)
        }
    }

    // 좋아요 상태를 서버와 동기화
    private fun handleLikeToggle(postData: PostData, isChecked: Boolean, holder: VH) {
        val uid = postData.uid ?: return

        if (isChecked) {
            // 좋아요 추가 요청
            retrofitService.likePost(postData.id, uid).enqueue(object : Callback<LikeResponse> {
                override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                    if (response.isSuccessful) {
                        Log.d("PostListAdapter", "Like added successfully")
                    } else {
                        Log.e("PostListAdapter", "Failed to add like: ${response.errorBody()?.string()}")
                        holder.binding.tbFavorite.isChecked = false // 실패 시 상태 복구
                    }
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    Log.e("PostListAdapter", "Network error while adding like: ${t.message}")
                    holder.binding.tbFavorite.isChecked = false // 실패 시 상태 복구
                }
            })
        } else {
            // 좋아요 삭제 요청
            retrofitService.unlikePost(postData.id, uid).enqueue(object : Callback<LikeResponse> {
                override fun onResponse(call: Call<LikeResponse>, response: Response<LikeResponse>) {
                    if (response.isSuccessful) {
                        Log.d("PostListAdapter", "Like removed successfully")
                    } else {
                        Log.e("PostListAdapter", "Failed to remove like: ${response.errorBody()?.string()}")
                        holder.binding.tbFavorite.isChecked = true // 실패 시 상태 복구
                    }
                }

                override fun onFailure(call: Call<LikeResponse>, t: Throwable) {
                    Log.e("PostListAdapter", "Network error while removing like: ${t.message}")
                    holder.binding.tbFavorite.isChecked = true // 실패 시 상태 복구
                }
            })
        }
    }

    // 날짜 포맷팅
    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    // 게시물 추가
    fun addPost(post: PostData) {
        itemList.add(post)
        notifyItemInserted(itemList.size - 1)
    }

    // 게시물 삭제
    fun removePost(postId: Int) {
        val position = itemList.indexOfFirst { it.id == postId }
        if (position != -1) {
            itemList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
