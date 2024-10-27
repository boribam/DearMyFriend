package com.bbam.dearmyfriend.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.data.GeocodeResponse
import com.bbam.dearmyfriend.data.MapItem
import com.bbam.dearmyfriend.databinding.FragmentMapBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapFragment: Fragment(), OnMapReadyCallback {

    private lateinit var binding: FragmentMapBinding
    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource

    private val markerList = mutableListOf<Marker>() // 마커 리스트
    private val hospitalList = mutableListOf<MapItem>() // 병원 데이터를 여기에 로드

    // dothome 서버와 네이버 지도 API에 사용할 각각의 RetrofitService 인스턴스
    private val dothomeService by lazy {
        RetrofitHelper.getDothomeRetrofitInstance().create(RetrofitService::class.java)
    }

    private val naverApiService by lazy {
        RetrofitHelper.getNaverApiRetrofitInstance().create(RetrofitService::class.java)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)

            if (fineLocationGranted || coarseLocationGranted) {
                initMapView()
            } else {
                Snackbar.make(binding.root, "위치 권한을 허용해주세요.", Snackbar.LENGTH_SHORT).show()
                initMapViewWithoutLocationTracking()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasPermission()) {
            Log.d("MapFragment", "권한 요청을 실행합니다.")
            locationPermissionRequest.launch(permissions)
        } else {
            // 권한이 이미 허용된 경우 지도 초기화
            Log.d("MapFragment", "권한이 이미 허용되었습니다.")
            initMapView()
        }
    }

    override fun onMapReady(p0: NaverMap) {
        this.naverMap = p0
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        // 지도가 준비되었을 때만 마커 추가
        updateMarkersInView(naverMap)

//        // 지도가 이동할 때마다 경계 내 마커 업데이트
//        naverMap.addOnCameraChangeListener { _, _ ->
//            if (!hospitalList.isNullOrEmpty()) {
//                updateMarkersInView(naverMap)
//            }
//        }

        // 병원 데이터를 가져오고, 주소로 좌표 변환 후 마커를 지도에 표시
        // 병원 데이터를 비동기로 로드하여 마커를 표시
        loadHospitalData()

    }

    private fun loadHospitalData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = dothomeService.getHospitalInformation().execute()
                if (response.isSuccessful) {
                    val hospitals = response.body() ?: emptyList()

                    withContext(Dispatchers.Main) {
                        hospitalList.clear()

                        val operatingHospitals = hospitals.filter { hospital ->
                            hospital.statusCode == 1
                        }

                        hospitalList.addAll(operatingHospitals)

                        if(naverMap != null) {
                            updateMarkersInView(naverMap!!)
                        }

                        operatingHospitals.forEach { hospital ->
                            if (!hospital.address.isNullOrEmpty()) {
                                convertAddressToCoordinates(hospital)
                            } else {
                                Log.e("MapFragment", "Hospital has no valid address: ${hospital.name}")
                            }
                        }

                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.e("MapFragment", "Failed to fetch data: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MapFragment", "Error loading data: ${e.message}")
                }
            }
        }
    }

    // 지도의 현재 화면에 있는 마커만 업데이트
    private fun updateMarkersInView(naverMap: NaverMap) {

        // naverMap 또는 hospitalList가 null일 경우 처리
        if (naverMap == null || hospitalList.isNullOrEmpty()) {
            Log.e("MapFragment", "NaverMap or hospitalList is null or empty")
            return
        }

        val currentBounds: LatLngBounds = naverMap.contentBounds

        // 기존 마커 중 화면에서 벗어난 마커는 비활성화
        markerList.forEach { marker ->
            if (!currentBounds.contains(marker.position)) {
                marker.map = null  // 지도에서 제거
            }
        }

        // 화면에 보이는 마커 중 아직 추가되지 않은 마커만 추가
        for (hospital in hospitalList) {
            // 병원의 위도와 경도가 null인지 확인
            if (hospital.latitude == null || hospital.longitude == null) {
                Log.e("MapFragment", "Hospital has invalid coordinates: ${hospital.name}")
                continue
            }

            val position = LatLng(hospital.latitude!!, hospital.longitude!!)
            if (currentBounds.contains(position)) {
                // 이미 추가된 마커인지 확인 후, 없으면 추가
                val existingMarker = markerList.find { it.position == position }
                if (existingMarker == null) {
                    addMarker(position, hospital)
                } else {
                    existingMarker.map = naverMap // 기존 마커 활성화
                }
            }
        }

        Log.d("MapFragment", "naverMap: $naverMap, hospitalList size: ${hospitalList?.size}")

    }

    // 마커를 추가하는 함수
    private fun addMarker(latLng: LatLng, hospital: MapItem) {
        val marker = Marker().apply {
            position = latLng
            map = naverMap  // 마커를 지도에 추가
            captionText = hospital.name ?: "병원 이름 없음"
            setOnClickListener {
                showBottomSheet(hospital)
                true
            }
        }
        markerList.add(marker) // 추가한 마커를 리스트에 저장
    }

    private fun convertAddressToCoordinates(hospital: MapItem) {
        val formattedAddress = hospital.address?.trim() ?: ""

        // 지오코딩 API 호출
        naverApiService.getCoordinates(formattedAddress).enqueue(object : Callback<GeocodeResponse> {
            override fun onResponse(call: Call<GeocodeResponse>, response: Response<GeocodeResponse>) {
                if (response.isSuccessful) {
                    val geocodeResponse = response.body()
                    val coordinates = geocodeResponse?.addresses?.firstOrNull()

                    if (coordinates != null) {
                        try {
                            val latLng = LatLng(coordinates.y.toDouble(), coordinates.x.toDouble())

                            // 변환된 좌표를 병원 객체에 저장
                            hospital.latitude = latLng.latitude
                            hospital.longitude = latLng.longitude

                            // 변환된 좌표로 마커 추가
                            addMarker(latLng, hospital)
                            Log.d("MapFragment", "Geocoding successful: ${hospital.name} at ${latLng.latitude}, ${latLng.longitude}")

                        } catch (e: Exception) {
                            Log.e("MapFragment", "Error parsing coordinates for ${hospital.name}: ${e.message}")
                        }
                    } else {
                        Log.e("GeocodeResponse", "No coordinates found for address: ${hospital.address}")
                    }
                } else {
                    Log.e("GeocodeResponse", "Geocode API failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<GeocodeResponse>, t: Throwable) {
                Log.e("GeocodeResponse", "Network error: ${t.message}")
            }
        })
    }

    private fun showBottomSheet(hospital: MapItem) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.fragment_hospital_bottom_sheet, null)

        val hospitalNameTextView: TextView = bottomSheetView.findViewById(R.id.hospital_name)
        val addressTextView: TextView = bottomSheetView.findViewById(R.id.tv_address)
        val phoneTextView: TextView = bottomSheetView.findViewById(R.id.tv_phone_num)

        // Set data
        hospitalNameTextView.text = hospital.name ?: "이름 없음"
        addressTextView.text = hospital.address ?: "주소 없음"
        phoneTextView.text = hospital.phoneNumber ?: "전화번호 없음"

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    // 권한을 거부했을 때 지도 초기화는 하되, 위치 추적은 하지 않음
    private fun initMapViewWithoutLocationTracking() {
        val fm = childFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as com.naver.maps.map.MapFragment?
            ?: com.naver.maps.map.MapFragment.newInstance().also {
                fm.beginTransaction().replace(R.id.map_fragment, it).commit()
            }

        mapFragment.getMapAsync { naverMap ->
            // 기본 위치로 지도 이동 (예: 서울)
            val seoul = LatLng(37.5665, 126.9780)
            val cameraUpdate = CameraUpdate.scrollAndZoomTo(seoul, 10.0)
            naverMap.moveCamera(cameraUpdate)

            // 위치 추적 모드 비활성화
            naverMap.locationTrackingMode = LocationTrackingMode.None
        }
    }

    private fun initMapView() {
        val fm = childFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map_fragment) as com.naver.maps.map.MapFragment?
            ?: com.naver.maps.map.MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map_fragment, it).commit()
            }

        mapFragment.getMapAsync(this)
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun hasPermission() : Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MapFragment", "권한이 없습니다: $permission")
                return false
            }
        }
        Log.d("MapFragment", "모든 권한이 허용되었습니다.")
        return true
    }
}