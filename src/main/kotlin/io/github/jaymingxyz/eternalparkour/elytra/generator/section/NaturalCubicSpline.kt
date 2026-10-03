package io.github.jaymingxyz.eternalparkour.elytra.generator.section

/**
 * A natural cubic spline through a set of points: a smooth curve made of one cubic polynomial per
 * interval, with zero curvature at both ends.
 *
 * Replaces Apache Commons Math's `SplineInterpolator`/`PolynomialSplineFunction`, which were the only
 * part of that library in use. The calculation follows commons-math3 3.6.1 (Apache License 2.0) step
 * by step, in the same floating-point operation order, so it produces bit-identical values: seeded
 * elytra courses stay exactly the same. `SplineEquivalenceTest` checks this against the library.
 *
 * @param x The x coordinates. At least 3, strictly increasing.
 * @param y The y coordinate for each x.
 */
class NaturalCubicSpline(x: DoubleArray, y: DoubleArray) {

    private val knots: DoubleArray = x.copyOf()

    /**
     * Polynomial coefficients per interval: `a + b*t + c*t^2 + d*t^3`, with `t` the distance from the
     * interval's first knot.
     */
    private val coefficients: Array<DoubleArray>

    init {
        require(x.size == y.size) { "x and y must have the same number of points" }
        require(x.size >= 3) { "At least 3 points are needed, got ${x.size}" }
        for (i in 1 until x.size) {
            require(x[i] > x[i - 1]) { "x must be strictly increasing (x[${i - 1}] = ${x[i - 1]}, x[$i] = ${x[i]})" }
        }

        val n = x.size - 1

        // Interval widths.
        val h = DoubleArray(n) { x[it + 1] - x[it] }

        // Forward sweep of the tridiagonal system for the second-derivative terms.
        val mu = DoubleArray(n)
        val z = DoubleArray(n + 1)
        for (i in 1 until n) {
            val g = 2.0 * (x[i + 1] - x[i - 1]) - h[i - 1] * mu[i - 1]
            mu[i] = h[i] / g
            z[i] = (3.0 * (y[i + 1] * h[i - 1] - y[i] * (x[i + 1] - x[i - 1]) + y[i - 1] * h[i]) /
                    (h[i - 1] * h[i]) - h[i - 1] * z[i - 1]) / g
        }

        // Back substitution; c[n] = 0 makes the spline natural.
        val b = DoubleArray(n)
        val c = DoubleArray(n + 1)
        val d = DoubleArray(n)
        for (j in n - 1 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
            b[j] = (y[j + 1] - y[j]) / h[j] - h[j] * (c[j + 1] + 2.0 * c[j]) / 3.0
            d[j] = (c[j + 1] - c[j]) / (3.0 * h[j])
        }

        coefficients = Array(n) { doubleArrayOf(y[it], b[it], c[it], d[it]) }
    }

    /**
     * @param v A value between the first and the last x coordinate, inclusive.
     * @return The spline's value at [v].
     */
    fun value(v: Double): Double {
        require(v >= knots.first() && v <= knots.last()) { "$v is outside [${knots.first()}, ${knots.last()}]" }

        var i = knots.binarySearch(v)
        if (i < 0) {
            i = -i - 2
        }
        if (i >= coefficients.size) {
            i-- // v is the last knot: use the last interval
        }

        val t = v - knots[i]
        val coefficient = coefficients[i]

        // Horner's method, skipping trailing zero coefficients as commons-math3 does.
        var degree = coefficient.size - 1
        while (degree > 0 && coefficient[degree] == 0.0) {
            degree--
        }

        var result = coefficient[degree]
        for (j in degree - 1 downTo 0) {
            result = t * result + coefficient[j]
        }
        return result
    }
}
