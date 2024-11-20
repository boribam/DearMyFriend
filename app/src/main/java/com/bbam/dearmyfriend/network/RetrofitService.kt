package com.bbam.dearmyfriend.network

import com.bbam.dearmyfriend.data.GeocodeResponse
import com.bbam.dearmyfriend.data.LikeResponse
import com.bbam.dearmyfriend.data.LoginResponse
import com.bbam.dearmyfriend.data.MapItem
import com.bbam.dearmyfriend.data.MemoDate
import com.bbam.dearmyfriend.data.PostData
import com.bbam.dearmyfriend.data.PostResponse
import com.bbam.dearmyfriend.data.RegisterResponse
import com.bbam.dearmyfriend.data.ScheduleModel
import com.bbam.dearmyfriend.data.ScheduleResponse
import com.bbam.dearmyfriend.data.SessionResponse
import com.bbam.dearmyfriend.data.UserResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
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

    // 일정 추가
    @FormUrlEncoded
    @POST("/dearfriend/addSchedule.php")
    fun addSchedule(
        @Field("uid") uid: String,
        @Field("memo") memo: String,
        @Field("date") date: String,
        @Field("isChecked") isChecked: Boolean
    ): Call<RegisterResponse>

    @GET("/dearfriend/getSchedule.php")
    fun getSchedule(
        @Query("uid") uid: String,
        @Query("date") date: String
    ): Call<ScheduleResponse>

    @FormUrlEncoded
    @POST("/dearfriend/updateSchedule.php")
    fun updateSchedule(
        @Field("id") id: Int,
        @Field("isChecked") isChecked: Boolean
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("/dearfriend/deleteSchedule.php")
    fun deleteSchedule(
        @Field("id") id: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("/dearfriend/getMemoDates.php")
    fun getMemoDates(
        @Field("uid") uid: String
    ): Call<List<MemoDate>>

    @FormUrlEncoded
    @POST("/dearfriend/getUidByEmail.php")
    fun getUidByEmail(
        @Field("email") email: String
    ): Call<RegisterResponse>

    @FormUrlEncoded
    @POST("/dearfriend/uploadPost.php")
    fun uploadPost(
        @Field("uid") uid: String,
        @Field("content") content: String,
        @Field("dateMillis") dateMillis: Long,
        @Field("dateFormatted") dateFormatted: String,
        @Field("imageUrls") imageUrls: String // JSON 문자열로 전송
    ): Call<PostResponse>

    @GET("/dearfriend/getPosts.php")
    fun getPosts(): Call<PostResponse>

    @FormUrlEncoded
    @POST("/dearfriend/likePost.php")
    fun likePost(
        @Field("post_id") postId: Int,
        @Field("uid") uid: String
    ): Call<LikeResponse>

    @FormUrlEncoded
    @POST("/dearfriend/unlikePost.php")
    fun unlikePost(
        @Field("post_id") postId: Int,
        @Field("uid") uid: String
    ): Call<LikeResponse>

    @FormUrlEncoded
    @POST("/dearfriend/getUserProfile.php")
    fun getUserProfile(
        @Field("uid") uid: String
    ): Call<Map<String, String>> // 그대로 유지

    @FormUrlEncoded
    @POST("/dearfriend/updateNickname.php")
    fun updateNickname(
        @Field("uid") uid: String,
        @Field("nickname") nickname: String
    ): Call<UserResponse> // UserResponse로 변경

    @FormUrlEncoded
    @POST("/dearfriend/updateProfileImage.php")
    fun updateProfileImage(
        @Field("uid") uid: String,
        @Field("profileImage") profileImage: String
    ): Call<UserResponse> // UserResponse로 변경

    @GET("/dearfriend/getLikedPosts.php")
    fun getLikedPosts(
        @Query("uid") uid: String
    ): Call<List<PostData>>

    @GET("/dearfriend/getUserPosts.php")
    fun getUserPosts(
        @Query("uid") uid: String
    ): Call<List<PostData>>

//    // 로그인 세션 확인
//    @GET("/dearfriend/checkSession.php")
//    fun checkSession(): Call<SessionResponse>

    @GET("/dearfriend/hospital.json")
    fun getHospitalInformation(): Call<List<MapItem>>

    // 네이버 지도 API의 Geocoding 기능을 호출해 도로명 주소를 좌표로 변환
    @Headers("X-NCP-APIGW-API-KEY-ID: xnxdkgomdx", "X-NCP-APIGW-API-KEY: WPRcRDIrvGY9WuKUe4SjsDkkytPybdOijqqqobNn")
    @GET("/map-geocode/v2/geocode")
    fun getCoordinates(
        @Query("query") address: String
    ): Call<GeocodeResponse>

}