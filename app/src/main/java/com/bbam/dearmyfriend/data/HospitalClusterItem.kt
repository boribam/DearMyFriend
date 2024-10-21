package com.bbam.dearmyfriend.data

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.MarkerManager

data class HospitalClusterItem(
    val position: LatLng,
    val hospital: MapItem
)
