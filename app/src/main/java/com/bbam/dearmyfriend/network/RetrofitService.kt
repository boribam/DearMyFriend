package com.bbam.dearmyfriend.network

import com.bbam.dearmyfriend.data.GeocodeResponse
import com.bbam.dearmyfriend.data.LoginResponse
import com.bbam.dearmyfriend.data.MapItem
import com.bbam.dearmyfriend.data.RegisterResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface RetrofitService {

    @FormUrlEncoded
    @POST("/dearfriend/insertUserDB.php")
    fun registerUser(
        @FieldMap dataPart: Map<String, String>
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("/dearfriend/login.php")
    fun loginUser(
        @Field("user_email") email: String,
        @Field("user_password") password: String
    ): Call<LoginResponse>

    @GET("/dearfriend/hospital.json")
    fun getHospitalInformation(): Call<List<MapItem>>

    // 네이버 지도 API의 Geocoding 기능을 호출해 도로명 주소를 좌표로 변환
    @Headers("X-NCP-APIGW-API-KEY-ID: xnxdkgomdx", "X-NCP-APIGW-API-KEY: WPRcRDIrvGY9WuKUe4SjsDkkytPybdOijqqqobNn")
    @GET("/map-geocode/v2/geocode")
    fun getCoordinates(
        @Query("query") address: String
    ): Call<GeocodeResponse>

}