import io.github.jaymingxyz.eternalparkour.elytra.generator.Generator
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.KnotDirector
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.Section
import org.bukkit.util.Vector
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Flies simulated players up generated climbs: each tick the climb sets the velocity, then the client runs
 * vanilla elytra physics.
 */
class ClimbTest {

    private class Flight(val maxOffset: Double, val meanSpeed: Double, val maxSpeed: Double, val final: Vector)

    // a player following the pipe looks at the centreline a little ahead of them
    private val followsThePipe = { section: Section, pos: Vector ->
        val ahead = ((pos.x - section.beginning.x).toInt() + 8).coerceIn(0, last(section))
        section.getPoint(ahead).clone().subtract(pos).normalize()
    }

    @Test
    fun aPlayerCanFollowThePipe() {
        var worst = 0.0
        repeat(300) { seed ->
            worst = max(worst, fly(climb(seed), Vector(1.5, -0.15, 0.0), followsThePipe).maxOffset)
        }

        // 3 is the narrowest pipe the radius setting allows
        assertTrue(worst < 3.0, "furthest from the centreline: $worst blocks")
    }

    @Test
    fun thePlayerSteers() {
        val look = Vector(1.0, 0.3, 1.0).normalize()
        val flight = fly(climb(0), Vector(1.5, -0.15, 0.0)) { _, _ -> look }

        // no pull towards the pipe: the player flies where they look
        val direction = flight.final.clone().normalize()
        assertTrue(direction.dot(look) > 0.99, "flies towards $direction, looks towards $look")
    }

    @Test
    fun settlesAtRocketSpeed() {
        repeat(50) { seed ->
            val flight = fly(climb(seed), Vector(1.0, -0.15, 0.0), followsThePipe)

            assertTrue(flight.maxSpeed < Generator.ROCKET_SPEED * 1.1, "top speed ${flight.maxSpeed}")
            assertTrue(flight.meanSpeed > Generator.ROCKET_SPEED * 0.85, "average speed ${flight.meanSpeed}")
        }
    }

    @Test
    fun keepsAFasterPlayersSpeedWithoutAddingToIt() {
        repeat(50) { seed ->
            val flight = fly(climb(seed), Vector(2.5, -0.3, 0.0), followsThePipe)

            assertTrue(flight.maxSpeed < 2.6 * 1.1, "top speed ${flight.maxSpeed}")
            assertTrue(flight.meanSpeed > 2.6 * 0.85, "average speed ${flight.meanSpeed}")
        }
    }

    private fun climb(seed: Int) = Section(Vector(0, 40, 0), Random(seed), KnotDirector.ASCENDING_VERTICAL_BIAS)

    private fun last(section: Section) = (section.end.x - section.beginning.x).toInt()

    private fun fly(section: Section, entry: Vector, look: (Section, Vector) -> Vector): Flight {
        val pos = section.getPoint(0).clone()
        var velocity = entry.clone()
        val speed = max(Generator.ROCKET_SPEED, velocity.length())
        var maxOffset = 0.0
        var maxSpeed = 0.0
        var speedTotal = 0.0
        var speedTicks = 0

        for (tick in 0 until 2000) {
            val direction = look(section, pos)
            velocity = glide(Generator.climbVelocity(direction, velocity, speed), direction)
            pos.add(velocity)

            val progress = (pos.x - section.beginning.x).toInt()
            if (progress >= last(section)) break

            maxOffset = max(maxOffset, pos.distance(section.getPoint(progress.coerceAtLeast(0))))
            maxSpeed = max(maxSpeed, velocity.length())
            if (tick >= 10) { // settled
                speedTotal += velocity.length()
                speedTicks++
            }
        }

        return Flight(maxOffset, speedTotal / speedTicks, maxSpeed, velocity)
    }

    // LivingEntity#updateFallFlyingMovement
    private fun glide(movement: Vector, look: Vector): Vector {
        val pitch = -asin(look.y)
        val lookHorizontal = sqrt(look.x * look.x + look.z * look.z)
        val movementHorizontal = sqrt(movement.x * movement.x + movement.z * movement.z)
        val cosSquared = cos(pitch) * cos(pitch)

        val result = movement.clone().add(Vector(0.0, 0.08 * (-1.0 + cosSquared * 0.75), 0.0))
        if (result.y < 0 && lookHorizontal > 0) {
            val d = result.y * -0.1 * cosSquared
            result.add(Vector(look.x * d / lookHorizontal, d, look.z * d / lookHorizontal))
        }
        if (pitch < 0 && lookHorizontal > 0) {
            val d = movementHorizontal * -sin(pitch) * 0.04
            result.add(Vector(-look.x * d / lookHorizontal, d * 3.2, -look.z * d / lookHorizontal))
        }
        if (lookHorizontal > 0) {
            result.add(Vector(
                (look.x / lookHorizontal * movementHorizontal - result.x) * 0.1,
                0.0,
                (look.z / lookHorizontal * movementHorizontal - result.z) * 0.1))
        }
        return result.multiply(Vector(0.99, 0.98, 0.99))
    }
}
