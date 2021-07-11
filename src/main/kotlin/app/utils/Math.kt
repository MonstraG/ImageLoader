package app.utils

import kotlin.math.pow
import kotlin.math.sqrt

fun sq(x: Int): Double {
    return (x * x).toDouble()
}

fun sq(x: Double): Double {
    return x.pow(2)
}

/**
 * Root of Sum of Squares; Pythogoras
 */
fun rss(a: Double, b: Double): Double {
    return sqrt(sq(a) + sq(b))
}

fun Double.fmt(digits: Int) = "%.${digits}f".format(this)
