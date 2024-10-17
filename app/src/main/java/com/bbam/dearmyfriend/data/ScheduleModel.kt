package com.bbam.dearmyfriend.data

data class ScheduleModel(
    val memo: String,
    val documentId: String,
    val date: String,
    var isChecked: Boolean = false
)
