package com.bbam.dearmyfriend.network

import com.bbam.dearmyfriend.data.GeocodeResponse
import com.bbam.dearmyfriend.data.MapItem
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface RetrofitService {

    @GET("/dearfriend/hospital.json")
    fun getHospitalInformation(): Call<List<MapItem>>

    // 네이버 지도 API의 Geocoding 기능을 호출해 도로명 주소를 좌표로 변환
    @Headers("X-NCP-APIGW-API-KEY-ID: xnxdkgomdx", "X-NCP-APIGW-API-KEY: WPRcRDIrvGY9WuKUe4SjsDkkytPybdOijqqqobNn")
    @GET("/map-geocode/v2/geocode")
    fun getCoordinates(
        @Query("query") address: String
    ): Call<GeocodeResponse>

}