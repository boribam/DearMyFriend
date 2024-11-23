package com.bbam.dearmyfriend.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.LoginActivity
import com.bbam.dearmyfriend.data.UserResponse
import com.bbam.dearmyfriend.databinding.FragmentMypageBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.storage.ktx.storage

class MypageFragment : Fragment() {

    lateinit var binding: FragmentMypageBinding

    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }
    private val sharedPreferences by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    private lateinit var currentUserUid: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMypageBinding.inflate(inflater, container, false)

        binding.pagerMypage.adapter = ViewPagerAdapter(this)
        binding.pagerMypage.setCurrentItem(0)

        TabLayoutMediator(
            binding.tabLayoutMypage,
            binding.pagerMypage
        ) { tab, position ->
            tab.text = if (position == 0) "작성한 게시물" else "좋아요한 게시물"
        }.attach()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserUid = sharedPreferences.getString("uid", null) ?: ""

        loadUserProfile()

        binding.ivProfileMypage.setOnClickListener { openGallery() }

        // 닉네임 변경 버튼 클릭
        binding.tvNicknameMypage.setOnClickListener {
            showNicknameDialog()
        }

        binding.btnLogout.setOnClickListener { showLogoutConfirmationDialog() }
    }

    private fun loadUserProfile() {
        retrofitService.getUserProfile(currentUserUid).enqueue(object : retrofit2.Callback<Map<String, String>> {
            override fun onResponse(
                call: retrofit2.Call<Map<String, String>>,
                response: retrofit2.Response<Map<String, String>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()
                    binding.tvNicknameMypage.text = data?.get("nickname") ?: "익명"
                    val profileImage = data?.get("profileImage")
                    if (!profileImage.isNullOrEmpty()) {
                        Glide.with(requireContext())
                            .load(profileImage)
                            .placeholder(R.drawable.user)
                            .into(binding.ivProfileMypage)
                    } else {
                        binding.ivProfileMypage.setImageResource(R.drawable.user)
                    }
                } else {
                    Snackbar.make(binding.root, "프로필 정보를 불러오지 못했습니다.", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {
                Log.e("프로필 정보", "서버오류: ${t.message}")
                Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        registerForActivityResult.launch(intent)
    }

    private val registerForActivityResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                if (imageUri != null) {
                    binding.ivProfileMypage.setImageURI(imageUri)
                    uploadProfileImageToServer(imageUri)
                }
            }
        }

    private fun uploadProfileImageToServer(imageUri: Uri) {
//        val profileImageUrl = "https://your-server-path/${currentUserUid}.jpg"

        // 프로필 이미지를 Firebase에 업로드하고 URL 저장
        val storageRef = com.google.firebase.ktx.Firebase.storage.reference.child("profile_images/$currentUserUid.jpg")
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                retrofitService.updateProfileImage(currentUserUid, downloadUrl.toString()).enqueue(object : retrofit2.Callback<UserResponse> {
                    override fun onResponse(call: retrofit2.Call<UserResponse>, response: retrofit2.Response<UserResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Snackbar.make(binding.root, "프로필 이미지를 변경했습니다.", Snackbar.LENGTH_SHORT).show()
                            Glide.with(requireContext()).load(downloadUrl.toString()).into(binding.ivProfileMypage)
                        } else {
                            Snackbar.make(binding.root, "프로필 이미지 변경 실패", Snackbar.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<UserResponse>, t: Throwable) {
                        Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
                    }
                })
            }
        }.addOnFailureListener {
            Snackbar.make(binding.root, "이미지 업로드 실패", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showNicknameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_nickname, null)
        val nicknameEditText = dialogView.findViewById<EditText>(R.id.nickname_change)

        nicknameEditText.setText(binding.tvNicknameMypage.text) // 현재 닉네임 설정
        nicknameEditText.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener true
            }
            false
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("변경") { _, _ ->
                val newNickname = nicknameEditText.text.toString()
                if (newNickname.isNotBlank()) {
                    updateNickname(newNickname)
                } else {
                    Toast.makeText(requireContext(), "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateNickname(newNickname: String) {
        retrofitService.updateNickname(currentUserUid, newNickname).enqueue(object : retrofit2.Callback<UserResponse> {
            override fun onResponse(call: retrofit2.Call<UserResponse>, response: retrofit2.Response<UserResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    binding.tvNicknameMypage.text = newNickname
                    Snackbar.make(binding.root, "닉네임이 변경되었습니다.", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "닉네임 변경 실패", Snackbar.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<UserResponse>, t: Throwable) {
                Snackbar.make(binding.root, "서버 오류: ${t.message}", Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout, null)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            .apply {
                dialogView.findViewById<LinearLayout>(R.id.btn_logout).setOnClickListener {
                    logout()
                    dismiss()
                }

                dialogView.findViewById<LinearLayout>(R.id.btn_logout_cancel).setOnClickListener {
                    dismiss()
                }
                show()
            }
    }

    private fun logout() {
        sharedPreferences.edit().clear().apply()

        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return if (position == 0) MypagePostFragment() else MypageFavoriteFragment()
        }
    }
}
