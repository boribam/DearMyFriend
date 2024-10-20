package com.bbam.dearmyfriend.data

import com.google.gson.annotations.SerializedName

data class GeocodeResponse(
    val status: String,
    val meta: Meta,
    val addresses: List<Address>,
    val errorMessage: String?
)

data class Meta(
    val totalCount: Int,
    val page: Int,
    val count: Int
)

data class Address(
    @SerializedName("roadAddress") val roadAddress: String?,
    @SerializedName("jibunAddress") val jibunAddress: String?,
    @SerializedName("englishAddress") val englishAddress: String?,
    @SerializedName("x") val x: String, // 경도
    @SerializedName("y") val y: String, // 위도
    @SerializedName("distance") val distance: Double?
)
