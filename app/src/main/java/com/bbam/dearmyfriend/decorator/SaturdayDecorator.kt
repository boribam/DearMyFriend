package com.bbam.dearmyfriend.decorator

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import java.time.DayOfWeek
import java.time.LocalDate

class SaturdayDecorator : DayViewDecorator {
    override fun shouldDecorate(day: CalendarDay?): Boolean {
        // 현재 그려질 날짜(day)가 일요일인가?  ** 여기서 day는 CalendarDay이기 때문에 일 만 가지고 있지 않고 년, 월, 일을 전부 가지고 있음
        val saturday: Int = LocalDate.of(day!!.year, day.month, day.day).with(DayOfWeek.SATURDAY).dayOfMonth
        return day.day == saturday
    }

    override fun decorate(view: DayViewFacade?) {
        // 위의 조건이 true이면 꾸미기
        view?.addSpan(ForegroundColorSpan(Color.BLUE))
    }
}