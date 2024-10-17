package com.bbam.dearmyfriend.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.data.ScheduleModel
import com.bbam.dearmyfriend.databinding.ScheduleListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

// ScheduleListAdapter에서 ListAdapter를 사용하여 효율적으로 아이템을 관리
class ScheduleListAdapter(val context: Context) : ListAdapter<ScheduleModel, ScheduleListAdapter.VH>(DiffCallback()) {

    private val firestore = FirebaseFirestore.getInstance()

    // ViewHolder 정의
    inner class VH(val binding: ScheduleListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScheduleModel) {
            binding.memo.text = item.memo
            binding.checkbox.isChecked = item.isChecked // 체크박스 상태 설정

            // 체크박스 상태 변경 리스너
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                // Firestore에 체크 상태 업데이트
                firestore.collection("schedules").document(item.documentId)
                    .update("isChecked", isChecked)
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "상태 업데이트 성공", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "상태 업데이트 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ScheduleListBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    // 아이템 개수 반환
    override fun getItemCount(): Int {
        return currentList.size
    }

    // ViewHolder와 데이터를 바인딩
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    // 아이템 삭제 메서드
    fun removeItem(position: Int) {
        val schedule = getItem(position)

        // Firestore에서 해당 메모 삭제
        firestore.collection("schedules").document(schedule.documentId)
            .delete()
            .addOnSuccessListener {
                val updatedList = currentList.toMutableList()
                updatedList.removeAt(position)
                submitList(updatedList) // 삭제 후 새로운 리스트 제출
                Toast.makeText(context, "메모가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "메모 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // DiffUtil을 통해 리스트 갱신을 효율적으로 처리
    class DiffCallback : DiffUtil.ItemCallback<ScheduleModel>() {
        override fun areItemsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            // 각 아이템의 고유 ID로 비교
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            // 아이템의 내용 비교
            return oldItem == newItem
        }
    }
}