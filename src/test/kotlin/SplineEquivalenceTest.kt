import io.github.jaymingxyz.eternalparkour.elytra.generator.section.KnotDirector
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.NaturalCubicSpline
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.bukkit.util.Vector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

/**
 * NaturalCubicSpline replaced commons-math3's SplineInterpolator. Elytra courses are generated from
 * seeds (time trials use a fixed one), so the replacement must give bit-identical results or seeded
 * courses would change. This compares both on the same inputs Section builds.
 */
class SplineEquivalenceTest {

    private val reference = SplineInterpolator()

    @Test
    fun testMatchesCommonsMathBitForBit() {
        var compared = 0

        for (seed in 0 until 5_000) {
            for (bias in intArrayOf(KnotDirector.DEFAULT_VERTICAL_BIAS, KnotDirector.ASCENDING_VERTICAL_BIAS)) {
                val (xs, ys, zs) = sectionInput(seed, bias)

                val expectedY = reference.interpolate(xs, ys)
                val expectedZ = reference.interpolate(xs, zs)
                val actualY = NaturalCubicSpline(xs, ys)
                val actualZ = NaturalCubicSpline(xs, zs)

                // the range Section samples
                for (x in xs[1].toInt()..xs[xs.size - 2].toInt()) {
                    assertBitEqual(expectedY.value(x.toDouble()), actualY.value(x.toDouble()), seed, x)
                    assertBitEqual(expectedZ.value(x.toDouble()), actualZ.value(x.toDouble()), seed, x)
                    compared++
                }

                // the outer extra points, including the very last knot
                assertBitEqual(expectedY.value(xs.first()), actualY.value(xs.first()), seed, xs.first().toInt())
                assertBitEqual(expectedY.value(xs.last()), actualY.value(xs.last()), seed, xs.last().toInt())
            }
        }

        assert(compared > 1_000_000) { "only compared $compared points" }
    }

    @Test
    fun testRejectsInvalidInput() {
        assertThrows<IllegalArgumentException> { NaturalCubicSpline(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 1.0)) }
        assertThrows<IllegalArgumentException> { NaturalCubicSpline(doubleArrayOf(0.0, 2.0, 1.0), doubleArrayOf(0.0, 1.0, 2.0)) }
        assertThrows<IllegalArgumentException> { NaturalCubicSpline(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(0.0, 1.0)) }

        val spline = NaturalCubicSpline(doubleArrayOf(0.0, 1.0, 2.0), doubleArrayOf(0.0, 1.0, 0.0))
        assertThrows<IllegalArgumentException> { spline.value(-0.5) }
        assertThrows<IllegalArgumentException> { spline.value(2.5) }
        assertEquals(1.0, spline.value(1.0))
    }

    // Same construction as Section: 5 knots from KnotDirector, plus one extra point at each end.
    private fun sectionInput(seed: Int, bias: Int): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val director = KnotDirector(Random(seed), bias)
        val knots = mutableListOf(Vector(seed * 1000, 150, -seed))
        repeat(4) { knots += knots.last().clone().add(director.nextOffset()) }

        knots.add(0, knots.first().clone().subtract(Vector(1, 0, 0)))
        knots += knots.last().clone().add(Vector(1, 0, 0))

        return Triple(
            knots.map { it.x }.toDoubleArray(),
            knots.map { it.y }.toDoubleArray(),
            knots.map { it.z }.toDoubleArray(),
        )
    }

    private fun assertBitEqual(expected: Double, actual: Double, seed: Int, x: Int) {
        assertEquals(expected.toRawBits(), actual.toRawBits()) { "seed $seed, x $x: expected $expected, got $actual" }
    }
}
