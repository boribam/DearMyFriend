package com.bbam.dearmyfriend.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.PostCommentActivity
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.databinding.RecyclerItemPostListBinding
import com.bbam.dearmyfriend.fragment.MypageFavoriteFragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostListAdapter(private val context: Context, private val itemList: MutableList<PostData>, // 변경된 데이터 클래스
                      private val itemClickListener: (PostData) -> Unit): RecyclerView.Adapter<PostListAdapter.VH>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    inner class VH(val binding: RecyclerItemPostListBinding) : ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RecyclerItemPostListBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val postData: PostData = itemList[position]

        // 닉네임 설정
        holder.binding.tvNickname.text = postData.nickname

        // 게시물 내용 설정
        holder.binding.tvContent.text = postData.content

        // 날짜 설정
        holder.binding.tvDate.text = formatDate(postData.dateMillis)

        // 프로필 사진 로드
        Glide.with(context).load(postData.profileImageUri).into(holder.binding.ivProfile)

        // 이미지 목록 표시
        val imageAdapter = ImageSliderAdapter(postData.imageUri ?: emptyList())
        holder.binding.viewPager.adapter = imageAdapter

        holder.binding.ivComment.setOnClickListener {
            val intent = Intent(context, PostCommentActivity::class.java).apply {
                putExtra("postId", postData.postId)
                putExtra("nickname", postData.nickname)
            }
            context.startActivity(intent)
        }

        // 좋아요 상태에 따라 토글 버튼 초기화
        checkIfLiked(postData, holder)

        holder.binding.tbFavorite.setOnCheckedChangeListener(null)

        // 토글 버튼 클릭 리스너
        holder.binding.tbFavorite.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                likePost(postData, holder.binding.root)  // 좋아요 저장
            } else {
                unlikePost(postData, holder.binding.root) // 좋아요 취소
            }
        }
    }

    private fun checkIfLiked(postData: PostData, holder: VH) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("favorites")
            .document(postData.postId)
            .get()
            .addOnSuccessListener { document ->
                holder.binding.tbFavorite.isChecked = document.exists()
            }
    }

    private fun likePost(postData: PostData, view: View) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("favorites")
            .document(postData.postId)
            .set(mapOf("postId" to postData.postId))
            .addOnSuccessListener {

            }.addOnFailureListener { e ->
                Log.e("favorites failed", "${e.message}")
            }
    }

    private fun unlikePost(postData: PostData, view: View) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).collection("favorites")
            .document(postData.postId)
            .delete()
            .addOnSuccessListener {

                val fragment = (context as? AppCompatActivity)?.supportFragmentManager?.findFragmentById(R.id.fragment_container)
                if (fragment is MypageFavoriteFragment) {
                    fragment.removePost(postData.postId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("favorites deleted failed", "${e.message}")
            }
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    fun addPost(post: PostData) {
        itemList.add(post)
        notifyItemInserted(itemList.size - 1) // 새로 추가된 항목의 위치를 알림
    }

    fun removePost(position: Int) {
        itemList.removeAt(position)
        notifyItemRemoved(position)  // 삭제된 항목의 위치를 알림
    }

    fun removeLikedPost(postId: String) {
        val position = itemList.indexOfFirst { it.postId == postId }
        if (position != -1) {
            itemList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}