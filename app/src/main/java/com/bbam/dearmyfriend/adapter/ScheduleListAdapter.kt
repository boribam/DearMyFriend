package com.bbam.dearmyfriend.adapter

import android.content.Context
import android.util.Log
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScheduleListAdapter(
    private val context: Context,
    private val retrofitService: RetrofitService
) : ListAdapter<ScheduleModel, ScheduleListAdapter.VH>(DiffCallback()) {

    inner class VH(private val binding: ScheduleListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScheduleModel) {
            binding.memo.text = item.memo
            binding.checkbox.isChecked = item.isCheckedAsBoolean() // Boolean으로 변환 후 설정

            // 체크박스 상태 변경 리스너
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (item.isCheckedAsBoolean() != isChecked) { // 기존 값과 동일하면 서버 호출 생략
                    item.isChecked = item.setCheckedAsInt(isChecked) // 1 또는 0으로 변환
                    updateScheduleStatus(item.id, isChecked)
                }
            }
        }

        private fun updateScheduleStatus(id: Int, isChecked: Boolean) {
            Log.d("UpdateSchedule", "Updating id: $id, isChecked: $isChecked") // 로그 추가
            retrofitService.updateSchedule(id, isChecked).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    Log.d("UpdateSchedule", "Response: ${response.body()}") // 응답 로그
                    if (response.isSuccessful && response.body()?.success == true) {
                        Snackbar.make(binding.root, "상태 업데이트 성공", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Log.e("UpdateSchedule", "Update failed: ${response.errorBody()?.string()}") // 오류 로그
                        Toast.makeText(context, "상태 업데이트 실패", Toast.LENGTH_SHORT).show()
                        binding.checkbox.isChecked = !isChecked // 실패 시 체크박스 복원
                    }
                }

                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Log.e("UpdateSchedule", "Request failed: ${t.message}") // 실패 로그
                    Toast.makeText(context, "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    binding.checkbox.isChecked = !isChecked // 실패 시 체크박스 복원
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ScheduleListBinding.inflate(LayoutInflater.from(context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    fun removeItem(position: Int) {
        val schedule = getItem(position)
        retrofitService.deleteSchedule(schedule.id.toString()).enqueue(object : Callback<RegisterResponse> {
            override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val updateList = currentList.toMutableList()
                    updateList.removeAt(position)
                    submitList(updateList) // 삭제 후 새로운 리스트 제출
                    Toast.makeText(context, "메모가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "메모 삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                Toast.makeText(context, "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class DiffCallback : DiffUtil.ItemCallback<ScheduleModel>() {
        override fun areItemsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScheduleModel, newItem: ScheduleModel): Boolean {
            return oldItem == newItem
        }
    }
}
