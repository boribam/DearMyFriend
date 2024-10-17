package com.bbam.dearmyfriend.fragment

import android.app.AlertDialog
import android.os.Bundle
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
import com.bbam.dearmyfriend.adapter.ScheduleListAdapter
import com.bbam.dearmyfriend.data.ScheduleModel
import com.bbam.dearmyfriend.databinding.FragmentCalendarBinding
import com.bbam.dearmyfriend.decorator.EventDecorator
import com.bbam.dearmyfriend.decorator.SaturdayDecorator
import com.bbam.dearmyfriend.decorator.SundayDecorator
import com.bbam.dearmyfriend.decorator.TodayDecorator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay

class CalendarFragment : Fragment() {

    private val binding by lazy { FragmentCalendarBinding.inflate(layoutInflater) }
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    var itemList: MutableList<ScheduleModel> = mutableListOf()
    private var selectedDate: CalendarDay? = null // 선택된 날짜를 저장할 변수

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
        fetchSchedules() // Firestore에서 사용자 일정 불러오기
        setupSwipeToDelete() // 스와이프 기능 설정
        updateEventDecorator()

        binding.fab.setOnClickListener {
            showAddScheduleDialog()
        }

        // 앱이 시작될 때 현재 날짜의 메모를 가져옵니다.
        selectedDate = CalendarDay.today()
        fetchSchedules() // 현재 날짜의 메모를 불러옵니다.
        updateEventDecorator()

        return binding.root
    }

    private fun setupRecyclerView() {
        val scheduleListAdapter = ScheduleListAdapter(requireContext())
        binding.recyclerViewSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSchedule.adapter = scheduleListAdapter

        // Adapter에 초기 데이터 설정
        scheduleListAdapter.submitList(itemList)
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

        val dateToUse = selectedDate ?: CalendarDay.today()
        val formattedDate = String.format(dateFormat, dateToUse.year, dateToUse.month, dateToUse.day)

        val uid = auth.currentUser?.uid // 현재 로그인된 사용자의 UID를 가져옴
        if (uid != null) {
            firestore.collection("schedules")
                .whereEqualTo("uid", uid) // UID에 따라 필터링
                .whereEqualTo("date", formattedDate) // 선택된 날짜에 해당하는 메모만 가져오기
                .get()
                .addOnSuccessListener { documents ->
                    itemList.clear() // 기존 리스트 비우기
                    for (document in documents) {
                        val memo = document.getString("memo") ?: continue
                        val isChecked = document.getBoolean("isChecked") ?: false
                        val documentId = document.id // 문서 ID 저장
                        itemList.add(ScheduleModel(memo, documentId, formattedDate, isChecked)) // ScheduleModel에 추가
                    }
                    binding.recyclerViewSchedule.adapter?.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "메모 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAddScheduleDialog() {

        val dateToUse = selectedDate ?: CalendarDay.today()

        val formattedDate = String.format(dateFormat, dateToUse.year, dateToUse.month, dateToUse.day)

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_memo, null)
        val editTextMemo = dialogView.findViewById<EditText>(R.id.add_memo)
        val selectedDateText =  dialogView.findViewById<TextView>(R.id.selectedDateShow)
        selectedDateText.text = "${formattedDate}"

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
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val memoData = hashMapOf(
                        "memo" to memo,
                        "uid" to uid,
                        "date" to formattedDate,
                        "isChecked" to false
                    )

                    firestore.collection("schedules")
                        .add(memoData)
                        .addOnSuccessListener { documentReference ->
                            itemList.add(ScheduleModel(memo, documentReference.id, formattedDate))
                            binding.recyclerViewSchedule.adapter?.notifyDataSetChanged()

                            // 도트 업데이트
                            updateEventDecorator()

                            dialog.dismiss()
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "메모 추가 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }else {
                    Toast.makeText(requireContext(), "사용자 로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                }

            } else {
                Toast.makeText(requireContext(), "메모를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

    }

    private fun updateEventDecorator(dateToRemove: String? = null) {
        val datesWithSchedule = mutableSetOf<CalendarDay>()

        firestore.collection("schedules")
            .whereEqualTo("uid", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val dateString = document.getString("date")
                    val dateParts = dateString?.split("-")
                    if (dateParts != null  && dateParts.size == 3) {
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
                        val monthToRemove = datePartsToRemove[1].toIntOrNull() // toIntOrNull로 변환
                        val dayToRemove = datePartsToRemove[2].toIntOrNull() // toIntOrNull로 변환

                        if (yearToRemove != null && monthToRemove != null && dayToRemove != null) {
                            datesWithSchedule.removeIf {
                                it.year == yearToRemove && it.month == monthToRemove && it.day == dayToRemove
                            }
                        }
                    }
                }
                // EventDecorator가 초기화되어 있는지 확인
                if (::eventDecorator.isInitialized) {
                    binding.calendarView.removeDecorator(eventDecorator)
                }

                val color = ContextCompat.getColor(requireContext(), R.color.signiture)
                eventDecorator = EventDecorator(color, datesWithSchedule)
                binding.calendarView.addDecorator(EventDecorator(color, datesWithSchedule))
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

                firestore.collection("schedules").document(scheduleItem.documentId).delete()
                    .addOnSuccessListener {
                        itemList.removeAt(position)
                        binding.recyclerViewSchedule.adapter?.notifyDataSetChanged()

                        updateEventDecorator(dateToRemove)
                    }.addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "삭제 실패 : ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        ItemTouchHelper(itemTouchHelper).attachToRecyclerView(binding.recyclerViewSchedule)
    }
}