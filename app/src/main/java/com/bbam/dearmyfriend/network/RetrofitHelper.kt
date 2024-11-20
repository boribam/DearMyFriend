package com.bbam.dearmyfriend.network

import com.google.gson.GsonBuilder
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

            var gson = GsonBuilder().setLenient().create()

            val retrofit = Retrofit.Builder().run {
                baseUrl(DOTHOME_BASE_URL)
                addConverterFactory(GsonConverterFactory.create(gson))
                build()
            }
            return retrofit
        }
    }
}