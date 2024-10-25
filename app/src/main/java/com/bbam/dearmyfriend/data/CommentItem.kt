package com.bbam.dearmyfriend.data

data class CommentItem(
    var nickname: String = "",
    var comment: String = "", // 댓글 내용
    var date: String = "",
    var postId: String = "",
    var uid: String = ""
)
