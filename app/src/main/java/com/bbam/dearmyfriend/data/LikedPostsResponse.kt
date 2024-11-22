package com.bbam.dearmyfriend.data

data class LikedPostsResponse(
    val success: Boolean,
    val likedPosts: List<PostData> // PostData는 게시물 데이터를 나타냅니다.
)
