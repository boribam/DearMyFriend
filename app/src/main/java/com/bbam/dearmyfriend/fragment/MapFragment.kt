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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.data.GeocodeResponse
import com.bbam.dearmyfriend.data.MapItem
import com.bbam.dearmyfriend.databinding.FragmentMapBinding
import com.bbam.dearmyfriend.network.RetrofitHelper
import com.bbam.dearmyfriend.network.RetrofitService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
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

//    private lateinit var clusterer: Clusterer<NaverItem>

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

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

        if (!hasPermission()) {
            ActivityCompat.requestPermissions(requireActivity(), permissions, LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            initMapView()
        }
    }

    override fun onMapReady(p0: NaverMap) {
        this.naverMap = p0
        naverMap.locationSource = locationSource
        naverMap.uiSettings.isLocationButtonEnabled = true
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        fetchHospitalData(naverMap)

    }

    private fun fetchHospitalData(naverMap: NaverMap) {
        dothomeService.getHospitalInformation().enqueue(object : Callback<List<MapItem>> {
            override fun onResponse(call: Call<List<MapItem>>, response: Response<List<MapItem>>) {
                if (response.isSuccessful) {
                    val hospitalList = response.body()?.filter {
                        it.statusCode == 1 // 영업 상태가 "정상"인 병원만 필터링
                    } ?: emptyList()

                    // 도로명 주소로 좌표 변환 후 마커 추가
                    for (hospital in hospitalList) {
                        convertAddressToCoordinates(hospital)
                    }
                } else {
                    Log.e("HospitalFragment", "Failed to fetch data: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<MapItem>>, t: Throwable) {
                Log.e("HospitalFragment", "Error: ${t.message}")
            }
        })
    }

    private fun convertAddressToCoordinates(hospital: MapItem) {
        naverApiService.getCoordinates(hospital.address ?: "").enqueue(object :
            Callback<GeocodeResponse> {
            override fun onResponse(call: Call<GeocodeResponse>, response: Response<GeocodeResponse>) {
                if (response.isSuccessful) {
                    Log.d("GeocodeResponse", "Response: ${response.body()}")
                    val geocodeResponse = response.body()
                    val coordinates = geocodeResponse?.addresses?.firstOrNull()
                    if (coordinates != null) {
                        val latitude = coordinates.y.toDouble()
                        val longitude = coordinates.x.toDouble()
                        addMarkerToCluster(LatLng(latitude, longitude), hospital, naverMap)
                    }
                } else {
                    Log.e("HospitalFragment", "Failed to convert address: ${response.errorBody()?.string()}")
                    Log.e("GeocodeResponse", "Error response: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<GeocodeResponse>, t: Throwable) {
                Log.e("HospitalFragment", "Error: ${t.message}")
                Log.e("GeocodeResponse", "Network error: ${t.message}")
            }
        })
    }

    private fun addMarkerToCluster(position: LatLng, hospital: MapItem, naverMap: NaverMap) {
        val marker = Marker().apply {
            this.position = position
            this.map = naverMap
            this.captionText = hospital.name ?: "병원 이름 없음"
        }

        marker.setOnClickListener {
            showBottomSheet(hospital)
            true
        }
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

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                initMapView()  // 권한이 허용된 경우 지도 초기화
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                initMapView()  // 대략적인 위치 권한이 허용된 경우 지도 초기화
            }
            else -> {
                Snackbar.make(binding.root, "위치 권한을 허용해주세요.", Snackbar.LENGTH_SHORT).show()

                // 지도는 여전히 초기화하지만, 사용자 위치 추적을 비활성화
                initMapViewWithoutLocationTracking()
            }
        }
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
                return false
            }
        }
        return true
    }
}