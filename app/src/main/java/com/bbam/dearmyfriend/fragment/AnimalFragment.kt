package com.bbam.dearmyfriend.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.adapter.AnimalAdapter
import com.bbam.dearmyfriend.data.Animal
import com.bbam.dearmyfriend.databinding.FragmentAnimalBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnimalFragment : Fragment() {

    private var _binding: FragmentAnimalBinding? = null
    private val binding get() = _binding!!

    private val baseUrl = "http://apis.data.go.kr/1543061/abandonmentPublicSrvc"
    private val serviceKey =
        "oYPMl3as0eC4Kgx1694anb%2BPxlugNfjK0XJDOdm4hipn4vaY2ezJUpeO602BQv8hRkPYKRNgbwnlykNnJ2zI6w%3D%3D"

    private val animalList = mutableListOf<Animal>()
    private lateinit var animalAdapter: AnimalAdapter

    private val sidoList = mutableListOf<Pair<String, String>>() // 시도 리스트
    private val sigunguList = mutableListOf<Pair<String, String>>() // 시군구 리스트

    // 필드 변수로 선택된 값 저장
    private var selectedSido: String = ""
    private var selectedSigungu: String = ""
    private var selectedUpkind: String = ""

    private var startDate: String = getDefaultStartDate() // 기본 시작 날짜 (최근 3개월 전)
    private var endDate: String = getDefaultEndDate() // 기본 종료 날짜 (현재 날짜)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAnimalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView 설정
        animalAdapter = AnimalAdapter(animalList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = animalAdapter

        // RecyclerView 스크롤 리스너 추가 (페이지네이션 처리)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 현재 RecyclerView의 레이아웃 매니저 확인
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // 마지막 항목에 도달했는지 확인 (목록 끝에 도달하면)
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                    // 다음 페이지 로드
                    loadNextPage()
                }
            }
        })

        // 기본 시작 날짜와 종료 날짜를 설정하고, TextView에 반영
        startDate = getDefaultStartDate() // 기본 시작 날짜를 가져옴
        endDate = getDefaultEndDate()     // 기본 종료 날짜를 가져옴

        // 날짜를 "YYYY년 MM월 DD일" 형식으로 변환해서 TextView에 설정
        binding.tvStartDate.text = formatDateForDisplay(startDate)
        binding.tvEndDate.text = formatDateForDisplay(endDate)

        // Spinner 데이터 설정
        loadSidoData()
        loadUpkindData()

        // 시작 날짜 선택
        binding.tvStartDate.setOnClickListener {
            showDatePickerDialog(isStartDate = true)
        }

        // 종료 날짜 선택
        binding.tvEndDate.setOnClickListener {
            showDatePickerDialog(isStartDate = false)
        }

        // 시도 선택 시 시군구 데이터 로드
        binding.spinnerSido.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedSidoCode = sidoList[position].second
                loadSigunguData(selectedSidoCode) // 시도에 따른 시군구 데이터 로드
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무것도 선택되지 않았을 때 처리할 내용 (필요하다면 구현)
            }
        }


        // 검색 버튼 클릭 리스너 설정
        binding.btnSearch.setOnClickListener {
            try {
                val selectedSido = sidoList[binding.spinnerSido.selectedItemPosition].second
                val selectedSigungu = sigunguList[binding.spinnerSigungu.selectedItemPosition].second
                val selectedUpkind = binding.spinnerUpkind.selectedItem.toString()

                // 축종 코드 매핑
                val upkindCode = when (selectedUpkind) {
                    "개" -> "417000"
                    "고양이" -> "422400"
                    "기타" -> "429900"
                    else -> "417000" // 기본값: 개
                }

                // 검색 시작 시 ProgressBar 표시
                binding.progressbar.visibility = View.VISIBLE
                binding.tvNoResults.visibility = View.GONE // 검색결과 없음 메시지 숨기기

                Log.d("SearchButton", "SidoCode: $selectedSido, SigunguCode: $selectedSigungu, UpkindCode: $upkindCode, StartDate: $startDate, EndDate: $endDate")

                // 선택한 값이 유효할 때만 API 호출
                if (selectedSido.isNotEmpty() && selectedSigungu.isNotEmpty() && upkindCode.isNotEmpty()) {
                    filterData(selectedSido, selectedSigungu, upkindCode, startDate, endDate)
                } else {
                    Toast.makeText(requireContext(), "모든 검색 조건을 선택해 주세요", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ButtonClick", "Error during search: ${e.message}")
            }
        }
    }

    // 기본 시작 날짜 가져오기 (최근 3개월 전 날짜)
    private fun getDefaultStartDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -3) // 3개월 전
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d%02d%02d", year, month, day) // YYYYMMDD 형식
    }

    // 기본 종료 날짜 가져오기 (오늘 날짜)
    private fun getDefaultEndDate(): String {
        val calendar = Calendar.getInstance() // 현재 날짜
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%04d%02d%02d", year, month, day) // YYYYMMDD 형식
    }

    private fun formatDateForDisplay(date: String): String {
        try {
            // 문자열에서 연도, 월, 일을 추출
            val year = date.substring(0, 4)
            val month = date.substring(4, 6)
            val day = date.substring(6, 8)

            // 원하는 형식으로 반환
            return String.format("%s년 %s월 %s일", year, month, day)
        } catch (e: Exception) {
            // 오류 발생 시 기본값 반환 (예외 처리)
            e.printStackTrace()
            return "날짜 형식 오류"
        }
    }

    // 날짜 선택을 위한 DatePickerDialog 호출
    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // 선택한 날짜를 Calendar 객체로 설정
                calendar.set(selectedYear, selectedMonth, selectedDay)

                // 원하는 형식으로 날짜 변환
                val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)

                // 시작 날짜인지 종료 날짜인지에 따라 TextView에 표시
                if (isStartDate) {
                    startDate = String.format("%04d%02d%02d", selectedYear, selectedMonth + 1, selectedDay) // YYYYMMDD 포맷
                    binding.tvStartDate.text = formattedDate // YYYY년 MM월 DD일 포맷
                } else {
                    endDate = String.format("%04d%02d%02d", selectedYear, selectedMonth + 1, selectedDay) // YYYYMMDD 포맷
                    binding.tvEndDate.text = formattedDate // YYYY년 MM월 DD일 포맷
                }
            }, year, month, day
        )
        datePickerDialog.show()
    }

    // 시도 데이터를 로드하는 함수
    private fun loadSidoData() {
        val url = "$baseUrl/sido?serviceKey=$serviceKey&numOfRows=17&_type=json"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseData = response.body?.string()
                Log.d("Sido API Response", responseData ?: "No data")
                parseSidoData(responseData)
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API Error", e.message ?: "Error occurred")
            }
        })
    }

    private fun parseSidoData(data: String?) {
        data?.let {
            try {
                val jsonObject = JSONObject(it)
                val items = jsonObject.getJSONObject("response").getJSONObject("body").getJSONObject("items").getJSONArray("item")

                sidoList.clear()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val sidoName = item.getString("orgdownNm")
                    val sidoCode = item.getString("orgCd")
                    sidoList.add(Pair(sidoName, sidoCode))
                }

                activity?.runOnUiThread {
                    val sidoNames = sidoList.map { it.first }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sidoNames)
                    binding.spinnerSido.adapter = adapter
                }
            } catch (e: JSONException) {
                Log.e("JSON Error", "Error parsing JSON: ${e.message}")
            }
        }
    }

    // 시군구 데이터를 로드하는 함수
    private fun loadSigunguData(selectedSidoCode: String) {
        val url = "$baseUrl/sigungu?upr_cd=$selectedSidoCode&serviceKey=$serviceKey&_type=json"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseData = response.body?.string()
                Log.d("Sigungu API Response", responseData ?: "No data")
                parseSigunguData(responseData)
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("API Error", e.message ?: "Error occurred")
            }
        })
    }

    private fun parseSigunguData(data: String?) {
        data?.let {
            try {
                val jsonObject = JSONObject(it)
                val items = jsonObject.getJSONObject("response").getJSONObject("body").getJSONObject("items").getJSONArray("item")

                sigunguList.clear()

                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val sigunguName = item.getString("orgdownNm")
                    val sigunguCode = item.getString("orgCd")
                    sigunguList.add(Pair(sigunguName, sigunguCode))
                }

                activity?.runOnUiThread {
                    val sigunguNames = sigunguList.map { it.first }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sigunguNames)
                    binding.spinnerSigungu.adapter = adapter
                }
            } catch (e: JSONException) {
                Log.e("JSON Error", "Error parsing JSON: ${e.message}")
            }
        }
    }

    // 축종 데이터를 하드코딩하여 로드
    private fun loadUpkindData() {
        val upkindList = listOf("개", "고양이", "기타")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, upkindList)
        binding.spinnerUpkind.adapter = adapter
    }

    private var currentPage = 1 // 현재 페이지
    private var totalPage = 1   // 총 페이지 수

    private fun filterData(sido: String, sigungu: String, upkind: String, startDate: String, endDate: String) {
        val url = "$baseUrl/abandonmentPublic?serviceKey=$serviceKey&upr_cd=$sido&org_cd=$sigungu&upkind=$upkind&bgnde=$startDate&endde=$endDate&numOfRows=50&pageNo=$currentPage&_type=json"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.use { res ->
                    if (res.isSuccessful) {
                        // 네트워크 작업을 백그라운드 스레드에서 수행하고, 결과를 메인 스레드에서 처리
                        val responseData = res.body?.string() // 이 부분은 백그라운드 스레드에서 처리
                        activity?.runOnUiThread {
                            // 메인 스레드에서 UI 업데이트
                            binding.progressbar.visibility = View.GONE // 검색 완료 시 ProgressBar 숨기기
                            parseFilteredData(responseData)
                        }
                    } else {
                        activity?.runOnUiThread {
                            // 에러 발생 시 처리
                            binding.progressbar.visibility = View.GONE
                            Log.e("API Error", "Response failed with code: ${response.code}")
                        }
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                activity?.runOnUiThread {
                    // 실패 시 UI 스레드에서 처리
                    binding.progressbar.visibility = View.GONE
                    Log.e("API Error", "Error occurred: ${e.message}")
                }
            }
        })
    }

    private fun loadNextPage() {
        if (currentPage < totalPage) {
            currentPage++ // 다음 페이지로 이동
            filterData(selectedSido, selectedSigungu, selectedUpkind, startDate, endDate) // 데이터를 다시 요청
        }
    }


    private fun parseFilteredData(data: String?) {
        data?.let {
            try {
                val jsonObject = JSONObject(it)
                val body = jsonObject.getJSONObject("response").getJSONObject("body")

                if (body.has("items")) {
                    val items = body.get("items")

                    animalList.clear()

                    if (items is JSONObject && items.has("item")) {
                        val itemArray = items.get("item")

                        if (itemArray is JSONArray) {
                            for (i in 0 until itemArray.length()) {
                                val item = itemArray.getJSONObject(i)
                                val animal = parseAnimal(item)
                                animalList.add(animal)
                            }
                        } else if (itemArray is JSONObject) {
                            val animal = parseAnimal(itemArray)
                            animalList.add(animal)
                        }

                        activity?.runOnUiThread {
                            if (animalList.isEmpty()) {
                                binding.tvNoResults.visibility = View.VISIBLE // 결과가 없으면 메시지 표시
                            } else {
                                binding.tvNoResults.visibility = View.GONE // 결과가 있으면 메시지 숨김
                            }
                            animalAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.d("JSON Parsing", "No items available")
                        activity?.runOnUiThread {
                            binding.tvNoResults.visibility = View.VISIBLE // 결과가 없으면 메시지 표시
                        }
                    }
                } else {
                    Log.d("JSON Parsing", "No items field in the response")
                    activity?.runOnUiThread {
                        binding.tvNoResults.visibility = View.VISIBLE // 결과가 없으면 메시지 표시
                    }
                }
            } catch (e: JSONException) {
                Log.e("JSON Error", "Error parsing JSON: ${e.message}")
            }
        } ?: run {
            Log.e("JSON Error", "Response data is null")
            activity?.runOnUiThread {
                binding.tvNoResults.visibility = View.VISIBLE // 데이터가 null이면 메시지 표시
            }
        }
    }

    private fun parseAnimal(item: JSONObject): Animal {
        return Animal(
            thumbnailUrl = item.optString("popfile", ""),
            dateStart = item.optString("noticeSdt", ""),
            dateEnd = item.optString("noticeEdt", ""),
            kind = item.optString("kindCd", ""),
            age = item.optString("age", ""),
            happenPlace = item.optString("happenPlace", "")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}