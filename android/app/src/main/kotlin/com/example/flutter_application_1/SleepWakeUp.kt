package com.example.flutter_application_1

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun findHourAndMin(hour: Int, min: Int, repeatTime: Long): Pair<Int, Int> {

    // 현재 날짜와 시간을 가져옵니다.
    val now = LocalDateTime.now()

    // 입력된 시간으로 변경합니다.
    val time = now.withHour(hour).withMinute(min).withSecond(0)

    // 20분을 감소합니다.
    val newTime = time.minusMinutes(repeatTime)


    // LocalDateTime을 Date로 변환합니다.
    val date = Date.from(newTime.atZone(ZoneId.systemDefault()).toInstant())

    // hour와 min 추출 후 반환
    val calendar = Calendar.getInstance()
    calendar.time = date
    val newHour = calendar.get(Calendar.HOUR_OF_DAY)
    val newMin = calendar.get(Calendar.MINUTE)

    println("Hour: $newHour, Min: $newMin")

    return Pair(newHour, newMin)
}

