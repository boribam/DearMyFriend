package com.bbam.dearmyfriend.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.LoginActivity
import com.bbam.dearmyfriend.databinding.FragmentMypageBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

lateinit var fragmentMypagePost: MypagePostFragment
lateinit var fragmentMypageFavor: MypageFavoriteFragment

class MypageFragment : Fragment() {

    lateinit var binding: FragmentMypageBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserUid: String
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        fragmentMypagePost = MypagePostFragment()
        fragmentMypageFavor = MypageFavoriteFragment()

        binding.pagerMypage.adapter = viewPagerAdapter(this)
        binding.pagerMypage.setCurrentItem(0)

        var tabLayoutMediator: TabLayoutMediator = TabLayoutMediator(
            binding.tabLayoutMypage,
            binding.pagerMypage,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                when (position) {
                    0 -> tab.text = "작성한 게시물"
                    else -> tab.text = "좋아요한 게시물"
                }
            })
        tabLayoutMediator.attach()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        currentUserUid = auth.currentUser?.uid ?: ""

        loadImageProfile()
        loadNickname()

        binding.ivProfileMypage.setOnClickListener { openGallery() }

        // 닉네임 변경 버튼 클릭
        binding.tvNicknameMypage.setOnClickListener {
            showNicknameDialog()
        }

        binding.btnLogout.setOnClickListener { showLogoutConfirmationDialog() }
    }
    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<LinearLayout>(R.id.btn_logout).setOnClickListener {
            logout()
            dialog.dismiss()
        }

        dialogView.findViewById<LinearLayout>(R.id.btn_logout_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun logout() {
        val auth = FirebaseAuth.getInstance()
        auth.signOut()

        // 로그인 화면으로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK  // 기존 액티비티 제거
        startActivity(intent)
        requireActivity().finish() // 현재 액티비티 종
    }

    private fun loadImageProfile() {
        db.collection("users").document(currentUserUid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val imageUrl = document.getString("profileImage")
                if (imageUrl != null) {
                    Picasso.get().load(imageUrl).into(binding.ivProfileMypage)
                } else {
                    // 이미지 URL 이 없는 경우 기본 이미지로 대체
                    binding.ivProfileMypage.setImageResource(R.drawable.user)
                }
            } else {
                // 문서가 존재하지 않는 경우 기본 이미지로 대체
                binding.ivProfileMypage.setImageResource(R.drawable.user)
            }
        }.addOnFailureListener {
            binding.ivProfileMypage.setImageResource(R.drawable.user)
            Toast.makeText(requireContext(), "Firestore에서 데이터를 가져오지 못했습니다.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun loadNickname() {
        db.collection("users").document(currentUserUid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val nickname = document.getString("nickname") ?: "익명"
                binding.tvNicknameMypage.text = nickname // TextView에 닉네임 설정
            } else {
                binding.tvNicknameMypage.text = "익명" // 기본값 설정
            }
        }.addOnFailureListener {
            binding.tvNicknameMypage.text = "익명" // 기본값 설정
            Toast.makeText(context, "닉네임 로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        registerForActivityResult.launch(intent)
    }

    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                if (imageUri != null) {
                    binding.ivProfileMypage.setImageURI(imageUri)
                    uploadImageToFIrebase(imageUri)
                }
            }
        }

    private fun uploadImageToFIrebase(imageUri: Uri) {
        val storageRef = Firebase.storage.reference.child("profile_images/$currentUserUid.jpg")

        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                // Firestore의 users 컬렉션에서 현재 사용자 UID에 해당하는 문서 업데이트
                db.collection("users").document(currentUserUid)
                    .get().addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            // 기존에 프로필 이미지가 없었을 경우, URL 업데이트
                            db.collection("users").document(currentUserUid)
                                .update("profileImage", downloadUrl.toString())
                                .addOnSuccessListener {
                                    Snackbar.make(
                                        binding.root,
                                        "프로필 이미지를 변경했습니다.",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                    updatePostsProfileImage(downloadUrl.toString())
                                }.addOnFailureListener { e ->
                                    Log.e(
                                        "Firestore Update",
                                        "Failed to update profile image: ${e.message}"
                                    )
                                }
                        } else {
                            // 만약 문서가 없으면 새로운 문서 생성
                            val userInfo = hashMapOf(
                                "profileImage" to downloadUrl.toString()
                            )
                            db.collection("users").document(currentUserUid)
                                .set(userInfo)
                                .addOnSuccessListener {
                                    Snackbar.make(
                                        binding.root,
                                        "프로필 이미지를 변경했습니다.",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }.addOnFailureListener { e ->
                                    Log.e(
                                        "Firestore Set",
                                        "Failed to set profile image: ${e.message}"
                                    )
                                }
                        }
                    }
            }

        }.addOnFailureListener { e ->
            Log.e("Profile Upload Failed", "${e.message}")
            Snackbar.make(binding.root, "이미지 업로드 실패", Snackbar.LENGTH_SHORT).show()
        }
    }

    class viewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return fragmentMypagePost
                else -> return fragmentMypageFavor
            }
        }
    }

    private fun showNicknameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nickname, null)
        val nicknameEditText = dialogView.findViewById<EditText>(R.id.nickname_change)

        nicknameEditText.setText(binding.tvNicknameMypage.text)  // 현재 닉네임을 EditText에 설정
        nicknameEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (keyEvent != null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || i == EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener true
            }
            false
        }

        // AlertDialog 생성
        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("변경") { _, _ ->
                val newNickname = nicknameEditText.text.toString()
                if (newNickname.isNotBlank()) {
                    updateNickname(newNickname) // 닉네임 업데이트
                } else {
                    Toast.makeText(requireContext(), "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            }
            .show()

    }

    private fun updateNickname(newNickname: String) {
        // 먼저 중복된 닉네임 확인
        db.collection("users")
            .whereEqualTo("nickname", newNickname)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    if (task.result.isEmpty) {
                        // 중복된 닉네임이 없으면 닉네임 업데이트
                        val userDocument = db.collection("users").document(currentUserUid)
                        userDocument.update("nickname", newNickname)
                            .addOnSuccessListener {
                                binding.tvNicknameMypage.text = newNickname // UI 업데이트
                                Snackbar.make(binding.root, "닉네임이 변경되었습니다", Snackbar.LENGTH_SHORT)
                                    .show()
                                updatePostsNickname(newNickname)
                                updateCommentNickname(newNickname)
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(
                                    binding.root,
                                    "닉네임 변경 실패: ${e.message}",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                                Log.e("nickname", "${e.message}")
                            }
                    } else {
                        // 중복된 닉네임이 있을 경우
                        Snackbar.make(binding.root, "이미 사용 중인 닉네임입니다.", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Snackbar.make(binding.root, "닉네임 확인 실패", Snackbar.LENGTH_SHORT).show()
                }
            }
    }

    private fun updatePostsNickname(newNickname: String) {
        db.collection("posts")
            .whereEqualTo("uid", currentUserUid) // 현재 사용자 ID로 필터링
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("nickname", newNickname)
                        .addOnFailureListener { e ->
                            Log.e("Posts Update", "Failed to update nickname: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Posts Update", "Failed to get posts: ${e.message}")
            }
    }

    private fun updateCommentNickname(newNickname: String) {
        db.collection("posts")
            .get()
            .addOnSuccessListener { posts ->
                for (post in posts) {
                    val postId = post.id
                    val commentsRef = db.collection("posts").document(postId).collection("comments")

                    commentsRef.whereEqualTo("uid", currentUserUid).get()
                        .addOnSuccessListener { comments ->
                            for (comment in comments) {
                                comment.reference.update("nickname", newNickname)
                                    .addOnFailureListener { e ->
                                        Log.e("Comments update", "Failed to update nickname")
                                    }
                            }
                        }.addOnFailureListener { e ->
                            Log.e(
                                "Comments Fetch",
                                "Failed to get comments for post $postId: ${e.message}"
                            )
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("Posts Fetch", "Failed to get posts: ${e.message}")
            }
    }

    private fun updatePostsProfileImage(newImageUrl: String) {
        db.collection("posts")
            .whereEqualTo("uid", currentUserUid) // 현재 사용자 ID로 필터링
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("profileImageUri", newImageUrl) // 프로필 이미지 URL 업데이트
                        .addOnFailureListener { e ->
                            Log.e("Posts Update", "Failed to update profile image: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Posts Update", "Failed to get posts: ${e.message}")
            }
    }

}