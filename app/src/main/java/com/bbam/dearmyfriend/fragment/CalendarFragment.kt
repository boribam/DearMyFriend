package com.bbam.dearmyfriend.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.activity.LoginActivity
import com.bbam.dearmyfriend.adapter.ScheduleListAdapter
import com.bbam.dearmyfriend.data.MemoDate
import com.bbam.dearmyfriend.data.RegisterResponse
import com.bbam.dearmyfriend.data.ScheduleModel
import com.bbam.dearmyfriend.data.ScheduleResponse
import com.bbam.dearmyfriend.databinding.FragmentCalendarBinding
import com.bbam.dearmyfriend.decorator.EventDecorator
import com.bbam.dearmyfriend.decorator.SaturdayDecorator
import com.bbam.dearmyfriend.decorator.SundayDecorator
import com.bbam.dearmyfriend.decorator.TodayDecorator
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CalendarFragment : Fragment() {

    private val binding by lazy { FragmentCalendarBinding.inflate(layoutInflater) }
    private val retrofitService by lazy { RetrofitHelper.getInstance().create(RetrofitService::class.java) }
    private val sharedPreferences by lazy { requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    private lateinit var adapter: ScheduleListAdapter
    var itemList: MutableList<ScheduleModel> = mutableListOf()
    private var selectedDate: CalendarDay? = null  // 선택된 날짜를 저장할 변수
    private val dateFormat = "%04d-%02d-%02d" // YYYY-MM-DD 형식

    private lateinit var eventDecorator: EventDecorator // 전역 변수로 EventDecorator 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setupCalendar()
        setupRecyclerView()

        // UID 가져오기
        val uid = sharedPreferences.getString("uid", null)

        if (uid == null) {
            Log.e("CalendarFragment", "UID is null. Redirecting to LoginActivity.")

            // UID가 없으면 로그인 화면으로 이동
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finish()
        } else {
            Log.d("CalendarFragment", "UID retrieved from SharedPreferences: $uid")
            fetchSchedules() // 일정 불러오기
            updateEventDecorator() // 데코레이터 업데이트
        }

        // 앱이 시작될 때 현재 날짜의 메모를 가져옵니다.
        selectedDate = CalendarDay.today()
        fetchSchedules() // 서버에서 사용자 일정 불러오기

        setupSwipeToDelete() // 스와이프 기능 설정
        updateEventDecorator()

        binding.fab.setOnClickListener {
            showAddScheduleDialog()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ScheduleListAdapter(requireContext(), retrofitService)
        binding.recyclerViewSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSchedule.adapter = adapter

        // Adapter에 초기 데이터 설정
        adapter.submitList(itemList)
    }

    private fun setupCalendar() {
        binding.calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(2023, 1, 1))
            .commit()

        binding.calendarView.addDecorators(
            TodayDecorator(requireContext()),
            SaturdayDecorator(),
            SundayDecorator()
        )

        // 날짜 선택 리스너
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = date // 선택된 날짜 저장
            fetchSchedules()
        }
    }

    private fun fetchSchedules() {

        val uid = sharedPreferences.getString("uid", null)
        if (uid == null) {
            Log.e("CalendarFragment", "UID is null. Cannot fetch schedules.")
            return
        }

        val dateToUse = selectedDate ?: CalendarDay.today()
        val formattedDate = String.format(dateFormat, dateToUse.year, dateToUse.month, dateToUse.day)

        Log.d("fetchSchedules", "Requesting schedules for uid=$uid, date=$formattedDate")


        retrofitService.getSchedule(uid, formattedDate).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(
                call: Call<ScheduleResponse>,
                response: Response<ScheduleResponse>
            ) {
                Log.d("fetchSchedules", "Response: ${response.body()}") // 서버 응답 출력

                if (response.isSuccessful) {
                    val scheduleResponse = response.body()
                    Log.d("fetchSchedule", "Response: $scheduleResponse") // 응답 데이터 출력
                    if (scheduleResponse != null && scheduleResponse.success) {
                        itemList.clear()
                        itemList.addAll(scheduleResponse.data)
                        Log.d("fetchSchedule", "Updated itemList: $itemList") // 추가된 일정 확인
                        adapter.submitList(itemList)
                        adapter.notifyDataSetChanged() // 강제로 RecyclerView 갱신
                    } else {
                        Log.e("fetchSchedule", "서버 응답 오류: success=false")
                        Toast.makeText(requireContext(), "일정을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        adapter.submitList(itemList)
                        adapter.notifyDataSetChanged() // RecyclerView 갱신
                    }
                } else {
                    Log.e("fetchSchedule", "서버 응답 실패: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "서버 오류", Toast.LENGTH_SHORT).show()                }
            }

            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                Log.e("fetchSchedule", "서버 요청 실패: ${t.message}")
                Toast.makeText(requireContext(), "서버 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddScheduleDialog() {

        val dateToUse = selectedDate ?: CalendarDay.today()

        val formattedDate = String.format(dateFormat, dateToUse.year, dateToUse.month, dateToUse.day)

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_memo, null)
        val editTextMemo = dialogView.findViewById<EditText>(R.id.add_memo)
        val selectedDateText =  dialogView.findViewById<TextView>(R.id.selectedDateShow)
        selectedDateText.text = formattedDate

        editTextMemo.setOnEditorActionListener { textView, i, keyEvent ->
            if (keyEvent !=  null && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER || i == EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener true
            }
            false
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<LinearLayout>(R.id.btn_add).setOnClickListener {
            val memo = editTextMemo.text.toString()
            if (memo.isNotEmpty()) {
                val uid = sharedPreferences.getString("uid", null)
                if (uid != null) {
                    retrofitService.addSchedule(uid, memo, formattedDate, false).enqueue(object : Callback<RegisterResponse> {
                        override fun onResponse(
                            p0: Call<RegisterResponse>,
                            p1: Response<RegisterResponse>
                        ) {
                            if (p1.isSuccessful && p1.body()?.success == true) {
                                fetchSchedules()
                                updateEventDecorator() // 데코레이터 즉시 갱신
                                dialog.dismiss()
                            } else {
                                Toast.makeText(context, "일정 추가 실패", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(p0: Call<RegisterResponse>, p1: Throwable) {
                            Log.e("서버오류","서버오류: ${p1.message}")
                            Toast.makeText(context, "서버 오류: ${p1.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            } else {
                Snackbar.make(requireContext(), binding.root, "메모를 입력하세요", Snackbar.LENGTH_SHORT).show()
//                Toast.makeText(requireContext(), "메모를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

    }

    private fun updateEventDecorator(dateToRemove: String? = null) {
        val datesWithSchedule = mutableSetOf<CalendarDay>()
        val uid = sharedPreferences.getString("uid", null) // SharedPreferences에서 uid 가져오기

        if (uid != null) {
            retrofitService.getMemoDates(uid).enqueue(object : Callback<List<MemoDate>> {
                override fun onResponse(p0: Call<List<MemoDate>>, p1: Response<List<MemoDate>>) {
                    Log.d("updateEventDecorator", "Response: ${p1.body()}") // 서버 응답 확인
                    if (p1.isSuccessful) {
                        val datesFromServer = p1.body() ?: emptyList()
                        Log.d("updateEventDecorator", "Dates from server: $datesFromServer") // 데이터 확인

                        // 서버에서 가져운 날짜를 CalendarDay 객체로 변환
                        for (memoDate in datesFromServer) {
                            val dateParts = memoDate.date.split("-")
                            if (dateParts.size == 3) {
                                val year = dateParts[0].toInt()
                                val month = dateParts[1].toInt()
                                val day = dateParts[2].toInt()
                                datesWithSchedule.add(CalendarDay.from(year, month, day))
                            }
                        }

                        // 삭제된 날짜에 해당하는 도트 제거
                        dateToRemove?.let {
                            val datePartsToRemove = it.split("-")
                            if (datePartsToRemove.size == 3) {
                                val yearToRemove = datePartsToRemove[0].toIntOrNull() // toIntOrNull로 변환
                                val monthToRemove = datePartsToRemove[1].toIntOrNull()
                                val dayToRemove = datePartsToRemove[2].toIntOrNull()

                                if (yearToRemove != null && monthToRemove != null && dayToRemove != null) {
                                    datesWithSchedule.removeIf { calendarDay ->
                                        calendarDay.year == yearToRemove &&
                                                calendarDay.month == monthToRemove &&
                                                calendarDay.day == dayToRemove
                                    }
                                }
                            }
                        }

                        // 기존 데코레이터를 제거하고 새로운 데코레이터를 추가하여 최신 일정 반영
                        // EventDecorator가 초기화되어 있는지 확인
                        if (::eventDecorator.isInitialized) {
                            binding.calendarView.removeDecorator(eventDecorator)
                        }

                        val color = ContextCompat.getColor(requireContext(), R.color.signiture)
                        eventDecorator = EventDecorator(color, datesWithSchedule)
                        binding.calendarView.addDecorators(eventDecorator)
                    }
                }

                override fun onFailure(p0: Call<List<MemoDate>>, p1: Throwable) {
                    Log.e("updateEventDecorator", "Server error: ${p1.message}")
                    Toast.makeText(requireContext(), "서버 오류: ${p1.message}", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val scheduleItem = itemList[position]
                val dateToRemove = scheduleItem.date
                Log.d("deleteSchedule", "Attempting to delete: id=${scheduleItem.id}")

                retrofitService.deleteSchedule(scheduleItem.id.toString()).enqueue(object : Callback<RegisterResponse> {
                    override fun onResponse(
                        p0: Call<RegisterResponse>,
                        p1: Response<RegisterResponse>
                    ) {
                        Log.d("deleteSchedule", "Response: ${p1.body()}")

                        if (p1.isSuccessful && p1.body()?.success == true) {
                            val updateList = itemList.toMutableList()
                            updateList.removeAt(position)
                            // 아이템 삭제 후 UI 업데이트
                            itemList = updateList // itemList 업데이트
                            adapter.submitList(updateList) // 새 목록을 adapter에 전달
//                            binding.recyclerViewSchedule.adapter?.notifyDataSetChanged()

                            // 삭제된 날짜에 대한 decorator 업데이트
                            updateEventDecorator(dateToRemove)
                        } else {
                            Toast.makeText(requireContext(), "삭제 실패: 서버 오류", Toast.LENGTH_SHORT).show()
                            adapter.submitList(itemList) // 실패 시 원래 목록 복원
                        }
                    }

                    override fun onFailure(p0: Call<RegisterResponse>, p1: Throwable) {
                        Log.e("deleteSchedule", "서버 요청 실패: ${p1.message}")
                        Toast.makeText(requireContext(), "삭제 실패: ${p1.message}", Toast.LENGTH_SHORT).show()
                        adapter.submitList(itemList) // 실패 시 원래 목록 복원
                    }

                })
            }
        }

        ItemTouchHelper(itemTouchHelper).attachToRecyclerView(binding.recyclerViewSchedule)
    }
}