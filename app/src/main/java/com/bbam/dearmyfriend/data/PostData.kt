package com.bbam.dearmyfriend.data

data class PostData(
    var content: String = "", // 게시물 내용
    var imageUri: List<String>? = null, // 이미지 URI 목록
    var uid: String = "", // 작성자의 UID
    var userId: String? = "", // 작성자의 이메일
    var nickname: String? = "", // 작성자의 닉네임
    var dateMillis: Long = 0L, // 게시물 작성 날짜
    var dateFormatted: String = "",
    var profileImageUri: String? = null, // 작성자의 프로필 사진 URI
    var postId: String = ""
)
