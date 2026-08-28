package com.robertotorino.datstation

import java.util.Calendar

object EventManager {

    enum class SpecialEvent {
        CHRISTMAS,
        NEW_YEAR,
        MOONSHIE_BIRTHDAY,
        MOONSHIE_BIRTHDAY_NEAR,
        ROBERTO_BIRTHDAY,
        ROBERTO_BIRTHDAY_NEAR,
        NONE
    }

    var debugEvent: SpecialEvent? = null

    fun getCurrentEvent(): SpecialEvent {
        debugEvent?.let { return it }
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)
        val year = now.get(Calendar.YEAR)

        // Moonshie Birthday: May 1st
        if (month == Calendar.MAY && day == 1) return SpecialEvent.MOONSHIE_BIRTHDAY
        if (isBirthdayNear(month, day, Calendar.MAY, 1)) return SpecialEvent.MOONSHIE_BIRTHDAY_NEAR

        // Roberto Birthday: October 23rd
        if (month == Calendar.OCTOBER && day == 23) return SpecialEvent.ROBERTO_BIRTHDAY
        if (isBirthdayNear(month, day, Calendar.OCTOBER, 23)) return SpecialEvent.ROBERTO_BIRTHDAY_NEAR

        // New Year's Eve and Day: Dec 31 to Jan 2
        if ((month == Calendar.DECEMBER && day >= 31) || (month == Calendar.JANUARY && day <= 2)) {
            return SpecialEvent.NEW_YEAR
        }

        // Christmas Period: Black Friday (Nov) to Jan 5th
        val blackFridayDay = getBlackFriday(year)
        if (isInsideChristmasPeriod(now, blackFridayDay)) {
            return SpecialEvent.CHRISTMAS
        }

        return SpecialEvent.NONE
    }

    private fun isBirthdayNear(nowMonth: Int, nowDay: Int, targetMonth: Int, targetDay: Int): Boolean {
        if (nowMonth != targetMonth) return false
        val diff = targetDay - nowDay
        return diff in 1..7 // Within 7 days before
    }

    fun isInsideChristmasPeriod(
        now: Calendar = Calendar.getInstance(),
        blackFridayDay: Int = getBlackFriday(now.get(Calendar.YEAR))
    ): Boolean {
        val month = now.get(Calendar.MONTH)
        val day = now.get(Calendar.DAY_OF_MONTH)

        // November: From Black Friday onwards
        if (month == Calendar.NOVEMBER && day >= blackFridayDay) return true

        // December: All month
        if (month == Calendar.DECEMBER) return true

        // January: Up to Jan 5th
        if (month == Calendar.JANUARY && day <= 5) return true

        return false
    }

    private fun getBlackFriday(year: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.NOVEMBER, 1)

        // Find first Thursday
        var dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        while (dayOfWeek != Calendar.THURSDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        }

        // Add 3 weeks to get to the 4th Thursday (Thanksgiving)
        cal.add(Calendar.DAY_OF_MONTH, 21)

        // Black Friday is the day after Thanksgiving (4th Thursday)
        return cal.get(Calendar.DAY_OF_MONTH) + 1
    }
}
