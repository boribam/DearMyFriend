package com.bbam.dearmyfriend.decorator

import android.content.Context
import androidx.core.content.ContextCompat
import com.bbam.dearmyfriend.R
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade

class TodayDecorator(val context: Context) : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        // 파라미터 day : 현재 그려질 날짜 정보

        // 현재 그려질 날짜(day)가 오늘 인가?
        return day == CalendarDay.today()
    }

    override fun decorate(view: DayViewFacade?) {
        // 위의 shouldDecorate() 메소드의 리턴 값이 true 일때만 이곳이 실행됨

        // 배경 이미지 변경
        val drawable = ContextCompat.getDrawable(context, R.drawable.bg_today_border)
        view?.setBackgroundDrawable(drawable!!)
    }
}