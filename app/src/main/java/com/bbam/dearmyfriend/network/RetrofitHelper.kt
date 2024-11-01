package com.bbam.dearmyfriend.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import kotlin.math.log

class RetrofitHelper {

    companion object {
        // dothome 서버 요청을 위한 baseUrl
        private const val DOTHOME_BASE_URL = "http://boribam.dothome.co.kr/"

        // 네이버 지도 API 요청을 위한 baseUrl
        private const val NAVER_API_BASE_URL = "https://naveropenapi.apigw.ntruss.com/"

        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // dothome용 Retrofit 인스턴스 생성
        fun getDothomeRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(DOTHOME_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        // 네이버 API용 Retrofit 인스턴스 생성
        fun getNaverApiRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl(NAVER_API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        fun getInstance() : Retrofit {

            val cookieManager = CookieManager().apply {
                setCookiePolicy(CookiePolicy.ACCEPT_ALL)
            }

            // HttpLoggingInterceptor 추가 설정
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // OkHttpClient에 로깅 인터셉터 추가
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder().run {
                baseUrl(DOTHOME_BASE_URL)
                client(client) // 로깅이 포함된 OkHttpClient 사용
                addConverterFactory(GsonConverterFactory.create())
                build()
            }
            return retrofit
        }
    }
}