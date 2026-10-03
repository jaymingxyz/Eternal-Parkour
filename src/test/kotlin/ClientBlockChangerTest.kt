import io.github.jaymingxyz.eternalparkour.elytra.generator.section.ClientBlockChanger
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.ClientBlockChanger.Companion.chunkKey
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.util.Vector
import org.junit.jupiter.api.Test

class ClientBlockChangerTest {

    private val near = Vector(5, 100, 5) // chunk 0, 0
    private val ahead = Vector(40, 100, 5) // chunk 2, 0
    private val aside = Vector(5, 100, 40) // chunk 0, 2

    @Test
    fun testWaitsForChunk() {
        val changer = ClientBlockChanger()
        changer.queue(listOf(near, ahead), Material.STONE)

        // the client has no chunks yet: nothing may be sent, or it would be dropped for good
        assert(changer.drain { false }.isEmpty())

        val first = changer.drain { it == Chunk.getChunkKey(0, 0) }
        assert(first.size == 1 && first[0].keys == setOf(near))

        val second = changer.drain { true }
        assert(second.size == 1 && second[0].keys == setOf(ahead))

        assert(changer.drain { true }.isEmpty())
    }

    @Test
    fun testChunkKeyIncludesZ() {
        assert(near.chunkKey() == Chunk.getChunkKey(0, 0))
        assert(aside.chunkKey() == Chunk.getChunkKey(0, 2))

        val changer = ClientBlockChanger()
        changer.queue(listOf(near, aside), Material.STONE)

        val sent = changer.drain { it == Chunk.getChunkKey(0, 0) }
        assert(sent.size == 1 && sent[0].keys == setOf(near))
    }

    @Test
    fun testForget() {
        val changer = ClientBlockChanger()
        changer.queue(listOf(near, ahead), Material.STONE)
        changer.forget(listOf(ahead))

        val sent = changer.drain { true }
        assert(sent.size == 1 && sent[0].keys == setOf(near))
    }

    @Test
    fun testKeepsOrderAndMaterial() {
        // the style advances per block in queue order, which draws the spiral styles
        val blocks = (0..9).map { Vector(1, 100 + it, 1) }

        val changer = ClientBlockChanger()
        changer.queue(blocks, Material.YELLOW_CONCRETE)

        val sent = changer.drain { true }.single()
        assert(sent.keys.toList() == blocks)
        assert(sent.values.all { it == Material.YELLOW_CONCRETE })
    }
}
