package com.bbam.dearmyfriend.data

data class ScheduleModel(
    val id: Int,
    val uid: String,
    val date: String,
    val memo: String,
    var isChecked: Int, // 서버에서 숫자로 반환될 경우 Int로 수신
    val created_at: String
) {
    fun isCheckedAsBoolean() = isChecked == 1
    fun setCheckedAsInt(checked: Boolean) = if (checked) 1 else 0
}
