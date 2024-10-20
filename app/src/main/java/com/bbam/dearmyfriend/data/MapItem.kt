package com.bbam.dearmyfriend.data

import com.google.gson.annotations.SerializedName

data class MapItem(
    @SerializedName("번호") val id: Int,
    @SerializedName("사업장명") val name: String?,
    @SerializedName("도로명전체주소") val address: String?,
    @SerializedName("소재지전화") val phoneNumber: String?,
    @SerializedName("영업상태구분코드") val statusCode: Int,
    @SerializedName("상세영업상태명") val statusName: String?
)
