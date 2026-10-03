package io.github.jaymingxyz.eternalparkour.elytra.generator.section

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.style.Style
import io.papermc.paper.math.Position
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.util.Vector

/**
 * Pipes pending client-side block updates to the player without leaking server memory.
 *
 * Storage: [Vector] positions rather than [org.bukkit.block.Block]. Block lookup forces
 * a chunk load and pins the chunk in Paper's holder; we'd be loading thousands of chunks
 * per minute as the player flies. Vectors are 24 bytes and pin nothing.
 *
 * Timing: the client drops a block change for a chunk it hasn't received yet, and it is
 * never sent again. So blocks wait here, per chunk, until the player has that chunk
 * ([Player.isChunkSent]). Sending by distance instead lost every block past the client's
 * view distance, which made the course vanish about 100–150 blocks into the first climbing
 * section and hid most obstacles.
 *
 * Render: at send time each position gets the next style block (or its fixed material) and
 * the batch is sent with [Player.sendMultiBlockChange]. The server world is never read, so
 * no chunks are loaded.
 *
 * Concurrency: `queue()` runs from an async continuation; `check`/`forget`/`clear` from
 * the main thread. All of them synchronize on `this`; network work happens outside.
 */
class ClientBlockChanger {

    // chunk key -> positions in that chunk, in queue order; a null material takes the next style block
    private val toChange: MutableMap<Long, MutableMap<Vector, Material?>> = mutableMapOf()

    fun check(player: Player, style: Style) {
        for (blocks in drain(player::isChunkSent)) {
            render(player, blocks, style)
        }
    }

    /**
     * Removes and returns the pending blocks of every chunk the client has.
     */
    internal fun drain(isChunkSent: (Long) -> Boolean): List<Map<Vector, Material?>> {
        val ready = mutableListOf<Map<Vector, Material?>>()

        synchronized(this) {
            val iterator = toChange.iterator()
            while (iterator.hasNext()) {
                val (key, blocks) = iterator.next()

                if (isChunkSent(key)) {
                    ready += blocks
                    iterator.remove()
                }
            }
        }

        return ready
    }

    /**
     * Queues course blocks, keyed by chunk key, to be drawn in the player's style.
     */
    fun queue(new: Map<Long, Set<Vector>>) {
        IEP.log("Queued ${new.size} chunks")

        synchronized(this) {
            new.forEach { (key, vectors) ->
                val pending = toChange.getOrPut(key) { mutableMapOf() }
                vectors.forEach { pending[it] = null }
            }
        }
    }

    /**
     * Queues blocks of a fixed material, e.g. obstacles.
     */
    fun queue(vectors: Collection<Vector>, material: Material) {
        synchronized(this) {
            vectors.forEach { toChange.getOrPut(it.chunkKey()) { mutableMapOf() }[it] = material }
        }
    }

    /**
     * Drops blocks that haven't been sent yet, so a cleared section or obstacle can't appear later.
     */
    fun forget(vectors: Collection<Vector>) {
        synchronized(this) {
            for (vector in vectors) {
                val key = vector.chunkKey()
                val pending = toChange[key] ?: continue

                pending.remove(vector)
                if (pending.isEmpty()) {
                    toChange.remove(key)
                }
            }
        }
    }

    fun clear() {
        synchronized(this) {
            toChange.clear()
        }
    }

    /**
     * Sends a fixed-material spoof for the given positions right away (used by ObstacleGenerator
     * to un-draw obstacle blocks without dirtying the server world).
     */
    fun sendNow(player: Player, vectors: Collection<Vector>, material: Material) {
        if (vectors.isEmpty()) return
        val data = material.createBlockData()
        player.sendMultiBlockChange(vectors.associate { Position.block(it.blockX, it.blockY, it.blockZ) to data })
    }

    /**
     * One block-change send per chunk. `style.next()` per block preserves the upstream behaviour
     * where the style cycle advances per position, which is what produces the visible spiral
     * pattern on IncrementalStyle.
     */
    private fun render(player: Player, blocks: Map<Vector, Material?>, style: Style) {
        if (blocks.isEmpty()) return
        IEP.log("Displaying ${blocks.size} positions")

        val changes = HashMap<Position, BlockData>(blocks.size)
        for ((v, material) in blocks) {
            changes[Position.block(v.blockX, v.blockY, v.blockZ)] = (material ?: style.next()).createBlockData()
        }
        player.sendMultiBlockChange(changes)
    }

    companion object {

        /**
         * The key of the chunk this position is in, as used by [Player.isChunkSent].
         */
        fun Vector.chunkKey(): Long = Chunk.getChunkKey(blockX shr 4, blockZ shr 4)
    }
}
