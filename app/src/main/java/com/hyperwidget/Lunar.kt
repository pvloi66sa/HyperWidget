package com.hyperwidget

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

data class LunarDate(val day: Int, val month: Int, val year: Int, val leap: Boolean, val jd: Int)

/** Thuật toán âm lịch của Hồ Ngọc Đức, múi giờ UTC+7. */
object Lunar {
    private const val TZ = 7.0
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    private fun fl(x: Double): Int = floor(x).toInt()

    fun jd(d: Int, m: Int, y: Int): Int {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun newMoon(k: Int): Int {
        val kd = k.toDouble()
        val t = kd / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val r = PI / 180
        var j = 2415020.75933 + 29.53058868 * kd + 0.0001178 * t2 - 0.000000155 * t3
        j += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * r)
        val m = 359.2242 + 29.10535608 * kd - 0.0000333 * t2 - 0.00000347 * t3
        val mp = 306.0253 + 385.81691806 * kd + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * kd - 0.0016528 * t2 - 0.00000239 * t3
        val c = (0.1734 - 0.000393 * t) * sin(m * r) + 0.0021 * sin(2 * r * m) -
            0.4068 * sin(mp * r) + 0.0161 * sin(r * 2 * mp) - 0.0004 * sin(r * 3 * mp) +
            0.0104 * sin(r * 2 * f) - 0.0051 * sin(r * (m + mp)) - 0.0074 * sin(r * (m - mp)) +
            0.0004 * sin(r * (2 * f + m)) - 0.0004 * sin(r * (2 * f - m)) -
            0.0006 * sin(r * (2 * f + mp)) + 0.0010 * sin(r * (2 * f - mp)) +
            0.0005 * sin(r * (2 * mp + m))
        val dt = if (t < -11) 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        else -0.000278 + 0.000265 * t + 0.000262 * t2
        return fl(j + c - dt + 0.5 + TZ / 24)
    }

    private fun sunLong(jdn: Int): Int {
        val t = (jdn - 2451545.5 - TZ / 24) / 36525
        val t2 = t * t
        val r = PI / 180
        val m = 357.5291 + 35999.0503 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val dl = (1.9146 - 0.004817 * t - 0.000014 * t2) * sin(r * m) +
            (0.019993 - 0.000101 * t) * sin(r * 2 * m) + 0.00029 * sin(r * 3 * m)
        var l = (280.46645 + 36000.76983 * t + 0.0003032 * t2 + dl) * r
        l -= PI * 2 * floor(l / (PI * 2))
        return fl(l / PI * 6)
    }

    private fun month11(y: Int): Int {
        val off = jd(31, 12, y) - 2415021
        val k = fl(off / 29.530588853)
        var nm = newMoon(k)
        if (sunLong(nm) >= 9) nm = newMoon(k - 1)
        return nm
    }

    private fun leapOffset(a11: Int): Int {
        val k = fl((a11 - 2415021.076998695) / 29.530588853 + 0.5)
        var i = 1
        var arc = sunLong(newMoon(k + i))
        var last: Int
        do {
            last = arc
            i++
            arc = sunLong(newMoon(k + i))
        } while (arc != last && i < 14)
        return i - 1
    }

    fun convert(d: Int, m: Int, y: Int): LunarDate {
        val dn = jd(d, m, y)
        val k = fl((dn - 2415021.076998695) / 29.530588853)
        var monthStart = newMoon(k + 1)
        if (monthStart > dn) monthStart = newMoon(k)
        var a11 = month11(y)
        var b11 = a11
        var ly: Int
        if (a11 >= monthStart) {
            ly = y
            a11 = month11(y - 1)
        } else {
            ly = y + 1
            b11 = month11(y + 1)
        }
        val ld = dn - monthStart + 1
        val diff = (monthStart - a11) / 29
        var leap = false
        var lm = diff + 11
        if (b11 - a11 > 365) {
            val lo = leapOffset(a11)
            if (diff >= lo) {
                lm = diff + 10
                if (diff == lo) leap = true
            }
        }
        if (lm > 12) lm -= 12
        if (lm >= 11 && diff < 4) ly -= 1
        return LunarDate(ld, lm, ly, leap, dn)
    }

    fun dayName(jd: Int) = CAN[(jd + 9) % 10] + " " + CHI[(jd + 1) % 12]
    fun yearName(y: Int) = CAN[(y + 6) % 10] + " " + CHI[(y + 8) % 12]
}
