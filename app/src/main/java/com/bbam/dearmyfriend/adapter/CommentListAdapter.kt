package com.bbam.dearmyfriend.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.data.CommentItem
import com.bbam.dearmyfriend.databinding.RecyclerItemCommentBinding

class CommentListAdapter(
    private val context: Context,
    private val itemList: List<CommentItem>,
    private val itemClickListener: (CommentItem) -> Unit, // 클릭 리스너 추가
    private val onDeleteClickListener: (CommentItem) -> Unit
) : RecyclerView.Adapter<CommentListAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RecyclerItemCommentBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item: CommentItem = itemList[position]

        holder.bind(item) // bind 메서드 호출
        holder.itemView.setOnClickListener { itemClickListener(item) } // 클릭 리스너 설정

        holder.binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(item)
        }
    }

    inner class VH(val binding: RecyclerItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommentItem) {
            binding.tvNickname.text = item.nickname
            binding.tvComment.text = item.comment
            binding.tvDate.text = item.date
        }
    }

    private fun showDeleteConfirmationDialog(comment: CommentItem) {
        AlertDialog.Builder(context)
            .setMessage("이 댓글을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { dialog, _ ->
                onDeleteClickListener(comment) // 삭제 클릭 리스너 호출
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss() // 다이얼로그를 닫음
            }
            .create()
            .show()
    }
}