package com.bbam.dearmyfriend.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.databinding.ActivityPostCommentBinding

class PostCommentActivity : AppCompatActivity() {

    private val binding by lazy { ActivityPostCommentBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}