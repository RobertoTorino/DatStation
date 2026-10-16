package com.robertotorino.datstation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class EventManagerTest {

    private fun date(year: Int, month: Int, day: Int): Calendar =
        Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }

    @Test
    fun `christmas period starts on Black Friday`() {
        // 2026: Thanksgiving is Thursday 26 November, Black Friday the 27th.
        assertFalse(EventManager.isInsideChristmasPeriod(date(2026, Calendar.NOVEMBER, 26)))
        assertTrue(EventManager.isInsideChristmasPeriod(date(2026, Calendar.NOVEMBER, 27)))
    }

    @Test
    fun `christmas period covers December and ends on 5 January`() {
        assertTrue(EventManager.isInsideChristmasPeriod(date(2026, Calendar.DECEMBER, 1)))
        assertTrue(EventManager.isInsideChristmasPeriod(date(2026, Calendar.DECEMBER, 31)))
        assertTrue(EventManager.isInsideChristmasPeriod(date(2027, Calendar.JANUARY, 5)))
        assertFalse(EventManager.isInsideChristmasPeriod(date(2027, Calendar.JANUARY, 6)))
    }

    @Test
    fun `ordinary days are outside the christmas period`() {
        assertFalse(EventManager.isInsideChristmasPeriod(date(2026, Calendar.JUNE, 15)))
        assertFalse(EventManager.isInsideChristmasPeriod(date(2026, Calendar.NOVEMBER, 1)))
    }
}
