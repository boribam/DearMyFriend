package com.bbam.dearmyfriend.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.adapter.CommentListAdapter
import com.bbam.dearmyfriend.data.CommentItem
import com.bbam.dearmyfriend.databinding.ActivityPostCommentBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Date
import java.util.Locale

class PostCommentActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPostCommentBinding.inflate(layoutInflater) }

    var itemList: MutableList<CommentItem> = mutableListOf()
    private lateinit var nickname: String
    private lateinit var postId: String // 게시물 UID를 저장할 변수
    private lateinit var userId: String
    private lateinit var commentAdapter: CommentListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        userId = user?.uid ?: ""

        postId = intent.getStringExtra("postId") ?: ""

        commentAdapter = CommentListAdapter(
            this,
            itemList,
            { comment -> onCommentClick(comment)},
            { comment -> onCommentDelete(comment)}
        )
        binding.recyclerView.adapter = commentAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.submitBtn.setOnClickListener {
            val commentText = binding.etComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                uploadComment(commentText)

            } else {
                Snackbar.make(binding.root, "댓글을 입력하세요.", Snackbar.LENGTH_SHORT).show()
            }
        }

        fetchComments()
    }

    private fun uploadComment(commentText: String) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Firestore에서 사용자 정보 가져오기
        FirebaseFirestore.getInstance().collection("users").document(currentUserUid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val nickname = document.getString("nickname") ?: "익명" // 닉네임 가져오기

                    val newComment = CommentItem(nickname, commentText, getCurrentDate(), postId, userId)
                    itemList.add(newComment)
                    commentAdapter.notifyItemInserted(itemList.size - 1)

                    val commentData = hashMapOf(
                        "nickname" to nickname,
                        "comment" to commentText,
                        "date" to getCurrentDate(),
                        "postId" to postId,
                        "uid" to userId
                    )

                    // Firestore에 댓글 저장
                    FirebaseFirestore.getInstance().collection("posts").document(postId).collection("comments").add(commentData)
                        .addOnSuccessListener {
                            updateCommentCount(postId, 1) // 댓글 수 증가
                        }
                        .addOnFailureListener { e ->
                            Log.e("CommentStoreError", "댓글 저장 실패: ${e.message}")
                            Snackbar.make(binding.root, "댓글 저장 실패", Snackbar.LENGTH_SHORT).show()
                        }

                    binding.etComment.text.clear() // 댓글 입력창 비우기
                } else {
                    Snackbar.make(binding.root, "사용자 정보를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserFetchError", "사용자 정보 가져오기 실패: ${e.message}")
                Snackbar.make(binding.root, "사용자 정보 가져오기 실패", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun updateCommentCount(postId: String, increment: Long) {
        val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)
        postRef.update("commentCount", FieldValue.increment(increment))
    }

    private fun fetchComments() {
        Firebase.firestore.collection("posts").document(postId)
            .collection("comments")
            .get()
            .addOnSuccessListener { querySnapshot ->
                itemList.clear()
                for (document in querySnapshot) {
                    val comment = document.toObject(CommentItem::class.java)
                    Log.d("CommentDebug", "Comment: ${comment.comment}") // 로그 추가
                    itemList.add(comment)
                }
                commentAdapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                Log.e("comment delete failed", "${e.message}")
            }
    }

    private fun onCommentDelete(comment: CommentItem) {

        if (comment.uid == userId) {
            val commentQuery = Firebase.firestore.collection("posts")
                .document(postId)
                .collection("comments")
                .whereEqualTo("nickname", comment.nickname)
                .whereEqualTo("comment", comment.comment)
                .whereEqualTo("date", comment.date)

            commentQuery.get().addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    document.reference.delete().addOnSuccessListener {
                        val position = itemList.indexOf(comment)
                        if (position != -1) {
                            itemList.removeAt(position)
                            commentAdapter.notifyItemRemoved(position)
                            updateCommentCount(postId, -1)
                        }
                    }.addOnFailureListener { e ->
                        Log.e("CommentDeleteError", "댓글 삭제 실패: ${e.message}")
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("comment delete failed", "${e.message}")
            }
        } else {
            Snackbar.make(binding.root, "자신의 댓글만 삭제할 수 있습니다.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun onCommentClick(comment: CommentItem) {

    }

    private fun getCurrentDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }
}