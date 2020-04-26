package app.utils

import kotlin.math.pow

fun sq(x: Int): Double {
    return (x * x).toDouble()
}

fun sq(x: Double): Double {
    return x.pow(2)
}