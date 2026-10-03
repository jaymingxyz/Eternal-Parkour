package io.github.jaymingxyz.eternalparkour.elytra.generator.section

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.ClientBlockChanger.Companion.chunkKey
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.Section.Companion.KNOTS
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task
import io.papermc.paper.math.Position
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

/**
 * Represents a section of the total parkour of size [KNOTS].
 *
 * Memory model — addresses the 8 GB / 2 h leak users reported on long elytra runs:
 *
 *  - We store [Vector] coordinates, NOT Bukkit [org.bukkit.block.Block] instances. A
 *    Block lookup goes through World#getBlockAt which loads the chunk if it isn't
 *    resident; we'd be loading thousands of chunks per minute as the player flies and
 *    Paper holds onto them for a while (chunks have ~100 KB of palette/lighting state
 *    each). Vectors are 24 bytes and pin nothing.
 *  - Display blocks are drawn and cleared with [org.bukkit.entity.Player.sendMultiBlockChange],
 *    which only sends packets and never requires the chunk to be loaded server-side.
 *  - [blockMap] is dropped to null in [clear] so the GC can reclaim the per-section
 *    Vector lists; a single mature section without this fix retained ~10–50 KB of
 *    points + ~2500 short-lived chunk handles per generation cycle.
 */
class Section {

    private val director: KnotDirector
    private val knots: List<Vector>
    private val points: List<Vector>
    @Volatile
    private var builder: BukkitTask? = null
    val beginning get() = knots.first().clone()
    val end get() = knots.last().clone()

    /**
     * True when this section was generated with an upward [KnotDirector] bias — i.e. the
     * pipe climbs through it. Used by [io.github.jaymingxyz.eternalparkour.elytra.generator.Generator] to decide
     * whether to give the player a firework-rocket-style boost as they enter the
     * section so they can actually follow the climb on elytra.
     */
    val isAscending: Boolean

    constructor(start: Vector, random: Random, verticalBias: Int = KnotDirector.DEFAULT_VERTICAL_BIAS) {
        director = KnotDirector(random, verticalBias)
        knots = generateKnots(start)
        points = generatePoints()
        isAscending = verticalBias > 0
    }

    private constructor(section: Section, offset: Vector) {
        director = section.director
        knots = section.knots.map { it.clone().add(offset) }
        points = section.points.map { it.clone().add(offset) }
        isAscending = section.isAscending
    }

    fun getKnot(idx: Int) = knots[idx]
    fun getPoint(idx: Int) = points[idx]


    fun isNearPoint(vector: Vector, idx: Int, distance: Double = 6.0) =
        points[idx].distanceSquared(vector) < distance * distance

    fun clone(offset: Vector) = Section(this, offset)

    // Map chunk key -> set of block positions that section currently draws on the client.
    // Held until clear() so we can send AIR packets to undo the display when the section
    // rotates off the back of the player. Stored as Vectors (not Blocks) so the GC can
    // reclaim them without dragging chunk references into old-gen.
    @Volatile
    private var blockMap: MutableMap<Long, MutableSet<Vector>>? = null

    /**
     * Generates the block positions for this section asynchronously. Results are also
     * stashed in [blockMap] so [clear] can send AIR packets later. The returned future
     * resolves with the same map so the caller can hand it to [ClientBlockChanger.queue].
     */
    fun generate(
        settings: Settings,
        pointType: PointType
    ): CompletableFuture<MutableMap<Long, MutableSet<Vector>>> {
        val future = CompletableFuture<MutableMap<Long, MutableSet<Vector>>>()

        builder = Task.create(IEP.instance)
            .async()
            .execute {
                val currentBlocks: MutableMap<Long, MutableSet<Vector>> = mutableMapOf()

                for (point in points) {
                    val pts = pointType.getPoints(point, settings.radius)
                    for (pt in pts) {
                        // Raw arithmetic — no Location/Block alloc.
                        currentBlocks.getOrPut(pt.chunkKey()) { mutableSetOf() }.add(pt)
                    }
                }

                blockMap = currentBlocks
                future.complete(currentBlocks)
            }
            .run()

        return future
    }

    /**
     * Sends AIR packets for every position this section had drawn on the client, then
     * drops the stored map so the GC can collect it.
     *
     * <p>Uses [Player.sendMultiBlockChange] (grouped multi-block-change packets) instead of
     * the per-block `sendBlockChange(Location, BlockData)` tried first: the latter could race
     * ahead of chunk-data packets on a freshly-teleported client and get dropped, which was
     * the same mechanism behind the missing first-pipe-section bug. The server world is never
     * read, so no chunk is loaded.</p>
     *
     * Blocks still waiting in [blockChanger] for their chunk are dropped, so they can't
     * appear after the section is gone.
     *
     * MUST be called on the main thread.
     */
    fun clear(player: Player, blockChanger: ClientBlockChanger) {
        builder?.cancel()
        builder = null

        val localBlocks = blockMap ?: return
        blockMap = null  // drop the reference BEFORE iterating so a tick exception can't pin it

        blockChanger.forget(localBlocks.values.flatten())

        for ((chunkKey, vectors) in localBlocks) {
            if (vectors.isEmpty()) continue
            IEP.log("Clearing chunk $chunkKey")
            val changes = HashMap<Position, BlockData>(vectors.size)
            for (v in vectors) {
                changes[Position.block(v.blockX, v.blockY, v.blockZ)] = AIR
            }
            player.sendMultiBlockChange(changes)
        }
        localBlocks.clear()
    }

    private fun generatePoints(): List<Vector> {
        val knots = knots.toMutableList()

        // add points to force spline to have an angle of 0 at the start and end
        knots.add(0, knots.first().clone().subtract(Vector(EXTRA_POINTS_OFFSET, 0, 0)))
        knots += knots.last().clone().add(Vector(EXTRA_POINTS_OFFSET, 0, 0))

        val xs = knots.map { it.x }.toDoubleArray()
        val ys = knots.map { it.y }.toDoubleArray()
        val zs = knots.map { it.z }.toDoubleArray()

        val splineY = NaturalCubicSpline(xs, ys)
        val splineZ = NaturalCubicSpline(xs, zs)

        val points = mutableListOf<Vector>()

        val actualXFirst = xs.drop(EXTRA_POINTS_OFFSET).first().toInt()
        val actualXLast = xs.dropLast(EXTRA_POINTS_OFFSET).last().toInt()

        for (x in actualXFirst..actualXLast) {
            val y = splineY.value(x.toDouble())
            val z = splineZ.value(x.toDouble())

            points += Vector(x, y.toInt(), z.toInt())
        }

        return points
    }

    private fun generateKnots(start: Vector): List<Vector> {
        val knots = mutableListOf(start)

        repeat(KNOTS - 1) { knots += knots.last().clone().add(director.nextOffset()) }

        return knots
    }

    companion object {
        private const val KNOTS = 5
        private const val EXTRA_POINTS_OFFSET = 1
        // lazy: creating block data needs a running server, which unit tests don't have
        private val AIR: BlockData by lazy { Material.AIR.createBlockData() }
    }
}
