package com.bbam.dearmyfriend.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.data.RegisterResponse
import com.bbam.dearmyfriend.data.ScheduleModel
import com.bbam.dearmyfriend.databinding.ScheduleListBinding
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ScheduleListAdapter에서 ListAdapter를 사용하여 효율적으로 아이템을 관리
class ScheduleListAdapter(
    private val context: Context,
    private val retrofitService: RetrofitService
) : ListAdapter<ScheduleModel, ScheduleListAdapter.VH>(DiffCallback()) {

    inner class VH(private val binding: ScheduleListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScheduleModel) {
            binding.memo.text = item.memo
            binding.checkbox.isChecked = item.isChecked  // 체크박스 상태 설정

            // 체크박스 상태 변경 리스너
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                retrofitService.updateSchedule(item.documentId.toInt(), isChecked).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(
                        p0: Call<RegisterResponse>,
                        p1: Response<RegisterResponse>
                    ) {
                        if(p1.isSuccessful && p1.body()?.success == true) {
                            Snackbar.make(binding.root, "상태 업데이트 성공", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "상태 업데이트 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(p0: Call<RegisterResponse>, p1: Throwable) {
                        Toast.makeText(context, "서버 오류: ${p1.message}", Toast.LENGTH_SHORT).show()
                    }

                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleListAdapter.VH {
        val binding = ScheduleListBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: ScheduleListAdapter.VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    fun removeItem(position: Int) {
        val schedule = getItem(position)
        retrofitService.deleteSchedule(schedule.documentId).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(p0: Call<RegisterResponse>, p1: Response<RegisterResponse>) {
                if(p1.isSuccessful && p1.body()?.success == true) {
                    val updateList = currentList.toMutableList()
                    updateList.removeAt(position)
                    submitList(updateList) // 삭제 후 새로운 리스트 제출
                    Toast.makeText(context, "메모가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "메모 삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(p0: Call<RegisterResponse>, p1: Throwable) {
                Toast.makeText(context, "서버 오류: ${p1.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // DiffUtil을 통해 리스트 갱신을 효율적으로 처리
    class DiffCallback : DiffUtil.ItemCallback<ScheduleModel>() {
        override fun areItemsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            return oldItem.documentId == newItem.documentId
        }

        override fun areContentsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            return oldItem == newItem
        }

    }


}