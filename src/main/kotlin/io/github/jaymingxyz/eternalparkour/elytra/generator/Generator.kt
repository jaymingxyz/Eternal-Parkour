package io.github.jaymingxyz.eternalparkour.elytra.generator

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings.Companion.asStyle
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.ClientBlockChanger
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.KnotDirector
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.PointType
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.Section
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score.Companion.pretty
import io.github.jaymingxyz.eternalparkour.elytra.mode.Mode
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer
import io.github.jaymingxyz.eternalparkour.elytra.reward.Rewards
import io.github.jaymingxyz.eternalparkour.elytra.world.Divider
import io.github.jaymingxyz.eternalparkour.elytra.world.World
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematics
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.random.Random

private class RewardHandler(val mode: Mode) {

    /**
     * Achieved score rewards this run.
     */
    private val scoreRewards = mutableListOf<Int>()

    /**
     * Achieved interval rewards this run.
     */
    private val intervalRewards = mutableListOf<Int>()

    /**
     * Achieved one-time rewards this run.
     */
    private val oneTimeRewards = mutableListOf<Int>()

    fun checkScores(score: Int, generator: Generator) {
        ((score - 4)..score)
            .filter { it > 0 }
            .forEach { check(it, generator.player.player, generator) }
    }

    private fun check(score: Int, player: Player, generator: Generator) {
        for ((key, rewards) in Rewards.scoreRewards) {
            if (score < key || key in scoreRewards) continue

            scoreRewards += key

            rewards.forEach { it.execute(player, mode) }
        }

        for ((key, rewards) in Rewards.oneTimeRewards) {
            if (score < key || score in oneTimeRewards || key in generator.settings.rewards) continue

            oneTimeRewards += score
            generator.set { settings ->
                val newRewards = settings.rewards
                newRewards += key
                return@set Settings(settings, rewards = newRewards)
            }

            rewards.forEach { it.execute(player, mode) }
        }

        for ((key, rewards) in Rewards.intervalRewards) {
            if (score % key != 0 || score in intervalRewards) continue

            intervalRewards += score

            rewards.forEach { it.execute(player, mode) }
        }
    }

    fun clear() {
        scoreRewards.clear()
        oneTimeRewards.clear()
        intervalRewards.clear()
    }
}

private class Island(vector: Vector, schematic: Schematic) {

    private val blocks: List<Block> = schematic.paste(vector.toLocation(World.world))
    val playerSpawn: Vector
    val blockSpawn: Vector

    init {
        require(blocks.isNotEmpty())

        blocks.first { it.type == Material.DIAMOND_BLOCK }.let {
            playerSpawn = it.location.toVector().add(Vector(0.5, 0.0, 0.5))

            it.type = Material.AIR
        }
        blocks.first { it.type == Material.EMERALD_BLOCK }.let {
            blockSpawn = it.location.toVector().add(Vector(5, 0, 0))

            it.type = Material.AIR
        }
    }

    /**
     * Clears the island.
     */
    fun clear() {
        blocks.forEach { it.type = Material.AIR }
    }
}


open class Generator {

    // Visible to subclasses (ObstacleGenerator) so they can pipe their own client-side
    // updates through the same packet path used by the parkour pipe itself.
    protected val blockChanger = ClientBlockChanger()

    protected val sections = mutableMapOf<Int, Section>()

    private lateinit var rewardHandler: RewardHandler
    private lateinit var island: Island
    private var task: BukkitTask? = null
    // Marked true by remove(). tick() bails out at the top if it sees this, even if
    // BukkitTask.cancel() didn't take effect synchronously (we observed cases on Paper 26
    // where a tick that was already mid-dispatch from CraftScheduler.mainThreadHeartbeat
    // still fired after cancel returned). Belt-and-braces defense against the
    // "Sidebar is deleted" exception thrown when updateBoard runs post-leave.
    @Volatile
    private var stopped: Boolean = false
    // Handle to the delayed queue task for the FIRST section. We hold it so reset()
    // can cancel a stale pending queue — without that, a quick reset between
    // generate() and the delayed queue firing would let an orphan task push the old
    // section's block map into a freshly-cleared toChange.
    private var pendingFirstQueue: BukkitTask? = null
    private lateinit var leaderboard: Leaderboard
    private var start: Instant? = null
    private var pointType: PointType = PointType.CIRCLE
    private var random = Random(0)
    private var movementScore = 0.0
    // Tracks the section idx that we've already applied the INITIAL (with-sound) boost
    // to — guards the entry trigger so we don't replay the firework launch every tick
    // the player lingers near knot[0].
    private var boostedSectionIdx: Int = Int.MIN_VALUE
    // While set, every tick pushes the player where they look (continuous "firework rocket"
    // thrust). Cleared automatically when the player crosses past [sustainedBoostEndX]. A
    // one-shot push is not enough: elytra drag and gravity bleed it off in a couple of ticks.
    private var sustainedBoostSection: Section? = null
    private var sustainedBoostEndX: Double = Double.NEGATIVE_INFINITY
    // The speed (blocks per tick) the current climb holds the player at: their speed when it started,
    // but at least what a firework rocket gives.
    private var climbSpeed = ROCKET_SPEED

    /**
     * True while an ascending section is lifting the player, which sets their velocity every tick.
     */
    protected val isClimbing: Boolean
        get() = sustainedBoostSection != null

    // Per-tick scoreboard payload is fed through this cache so we don't reformat the
    // locale string list every server tick (was ~100 KB/s of churn at full tick rate).
    // updateBoardValues() in ElytraPlayer already memoizes its side; we just don't ask
    // for it more often than we must.

    lateinit var player: ElytraPlayer
        private set
    lateinit var settings: Settings
        private set
    var seed = 0
        protected set

    /**
     * Adds a player to the generator.
     * @param player The player to add.
     */
    fun add(player: ElytraPlayer) {
        IEP.log("Adding player to generator ${player.name}")

        this.player = player

        settings = player.load()
    }

    /**
     * Removes a player from the generator. Uses try-finally to guarantee task
     * cancellation and divider cleanup even if reset() throws — without this,
     * a generator that failed mid-reset could leak its BukkitTask + Section
     * tree forever (the task keeps a strong ref to the Generator instance,
     * which keeps every Section alive).
     */
    fun remove(player: ElytraPlayer) {
        IEP.log("Removing player from generator ${player.name}")

        // Set the stop flag BEFORE doing anything else — even if BukkitTask.cancel()
        // doesn't take effect synchronously, the next tick will bail at the top.
        stopped = true

        try {
            player.save(settings)
        } catch (t: Throwable) {
            IEP.logging.stack("Failed to save settings for ${player.name}", t)
        }

        // Cancel tick FIRST so no new work scheduled after we start tearing down.
        try { task?.cancel() } catch (_: Throwable) { /* task was never scheduled */ }
        task = null
        try { pendingFirstQueue?.cancel() } catch (_: Throwable) {}
        pendingFirstQueue = null

        try {
            reset(ResetReason.RESET, false)
        } catch (t: Throwable) {
            IEP.logging.stack("Failed to reset generator for ${player.name}", t)
        }

        try { island.clear() } catch (_: Throwable) { /* island may not have been built */ }
        try { blockChanger.clear() } catch (_: Throwable) {}

        Divider.remove(this)
    }

    /**
     * Initializes all the stuff.
     * @param mode The mode to use.
     * @param start The vector to spawn the island at.
     * @param point The point type to use.
     */
    open fun start(mode: Mode, start: Vector, point: PointType) {
        IEP.log("Starting generator at $start")

        rewardHandler = RewardHandler(mode)
        leaderboard = mode.leaderboard
        island = Island(start, Schematics.getSchematic(IEP.instance, "spawn-island"))
        pointType = point

        player.player.setPlayerTime(settings.time.toLong(), false)

        reset(ResetReason.RESET)

        task = Task.create(IEP.instance)
            .delay(5)
            .repeat(1)
            .execute(::tick)
            .run()
    }

    open fun getScore() = max(0.0, movementScore)

    fun getTime(): Instant = Instant.now().minusMillis(start?.toEpochMilli() ?: Instant.now().toEpochMilli())

    fun getHighScore() = leaderboard.getScore(player.uuid)

    /**
     * Ticks the generator.
     *
     * Per-tick allocation note: maxBy/minBy on the sections map allocates a Pair every
     * call. sections is bounded to ≤ 3 entries, so the allocation is small, but at 20 Hz
     * × 2 h that's ~140 k allocations. They're young-gen so GC handles them fine — we
     * keep the code readable rather than inlining a manual max/min scan.
     */
    protected open fun tick() {
        // If we got here after remove() set the stop flag — possible on Paper 26 when
        // a tick is mid-dispatch from CraftScheduler.mainThreadHeartbeat while we
        // call BukkitTask.cancel() from a higher-priority event handler — bail out.
        // Otherwise we'd be operating on dead state (board deleted, sections cleared)
        // and the next failed sendBoard would spam the console.
        if (stopped) {
            try { task?.cancel() } catch (_: Throwable) {}
            return
        }

        if (sections.isEmpty()) return  // post-reset gap before the first generate completes

        val (idx, section) = sections.maxBy { it.key }

        val pos = player.position
        val score = getScore()

        if (shouldScore()) {
            this.movementScore += player.player.velocity.x
        }

        if (Rewards.enabled) {
            rewardHandler.checkScores(score.toInt(), this)
        }

        blockChanger.check(player.player, settings.style.asStyle())

        updateBoard(score, getTime())
        updateInfo()

        if (start == null && score > 0) {
            start = Instant.now()
        }

        if (sections.size > 2) {
            val (minIdx, minSection) = sections.minBy { it.key }

            clear(minIdx, minSection)
        }

        val resetReason = shouldReset(player, pos)
        if (resetReason != null) {
            reset(resetReason)
            return
        }

        // Sustained boost: while the player is inside an ascending section, push them
        // every tick where they look. Without this, the initial boost bleeds off in 2-3
        // ticks under elytra drag/gravity and the player can't climb. Vanilla firework
        // rockets last ~20 ticks; we sustain across the whole ascending section.
        if (isClimbing) {
            if (pos.x >= sustainedBoostEndX) {
                sustainedBoostSection = null
                sustainedBoostEndX = Double.NEGATIVE_INFINITY
            } else {
                sustainAscendingBoost()
            }
        }

        // Initial boost (with firework sound + particles + a stronger one-shot push)
        // fires once when the player crosses into an ascending section's knot[0].
        // Subsequent ticks rely on the sustained-boost path above.
        //
        // Both triggers compare x only (the course always runs towards +x). They used to
        // need the player within 6 blocks of the knot on some tick, which a fast player
        // flying off-centre could skip in one tick: no boost, or no next section and the
        // course simply ended. shouldReset above already handles leaving the pipe.
        if (section.isAscending && idx != boostedSectionIdx && pos.x >= section.beginning.x) {
            applyAscendingBoost()
            sustainedBoostSection = section
            sustainedBoostEndX = section.end.x
            boostedSectionIdx = idx
        }

        if (pos.x >= section.getKnot(2).x) {
            generate(CompletableFuture.completedFuture(null))

            return
        }
    }

    /**
     * Replaces the upstream-IEP clone-and-teleport-up mechanic. Instead of warping the
     * player +200 Y (which breaks velocity and gliding state — players reported it
     * felt like a stutter), the climb works like a firework rocket held for the whole
     * section: it pushes where the player looks (see [climbVelocity]), with rocket
     * particles and sound.
     */
    private fun applyAscendingBoost() {
        val bukkit = player.player
        if (!bukkit.isOnline) return

        // No need to send queued blocks early here: tick() already sent every block whose
        // chunk the client has. Sending the rest (this used to flush the whole section) only
        // reached chunks the client hadn't loaded, which drop the change, so the course
        // vanished about 100–150 blocks into the climb.

        // The climb keeps the speed the player came in with, or gives them a rocket's if they were slower.
        climbSpeed = max(ROCKET_SPEED, bukkit.velocity.length())
        thrust()

        // Ensure the player is still gliding (in case they briefly stopped — they
        // were inside the pipe so this should be a no-op in practice).
        if (bukkit.inventory.chestplate.type == Material.ELYTRA && !bukkit.isGliding) {
            bukkit.isGliding = true
        }

        // Firework feel — particles + the canonical rocket launch sound.
        val loc = bukkit.location
        try {
            loc.world.spawnParticle(org.bukkit.Particle.FIREWORK, loc, 60, 0.6, 0.6, 0.6, 0.15)
        } catch (_: Throwable) { /* particle enum survived the 1.21 rename, but be safe */ }
        try {
            loc.world.playSound(loc, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f)
            loc.world.playSound(loc, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.4f)
        } catch (_: Throwable) {}

        onAscendingBoost()
    }

    /**
     * Applied every tick the player is inside an ascending section: without it, elytra drag and gravity
     * stop the player climbing within a few ticks.
     */
    private fun sustainAscendingBoost() {
        val bukkit = player.player
        if (!bukkit.isOnline) return

        thrust()

        if (bukkit.inventory.chestplate.type == Material.ELYTRA && !bukkit.isGliding) {
            bukkit.isGliding = true
        }

        // Occasional particle tail so the player can SEE the sustained thrust. Once
        // every 4 ticks (~5 times per second) keeps the rocket trail readable without
        // flooding the renderer.
        if ((bukkit.ticksLived and 3) == 0) {
            try {
                bukkit.world.spawnParticle(
                    org.bukkit.Particle.FIREWORK, bukkit.location, 6, 0.2, 0.2, 0.2, 0.02
                )
            } catch (_: Throwable) {}
        }
    }

    private fun thrust() {
        val bukkit = player.player
        bukkit.velocity = climbVelocity(bukkit.location.direction, bukkit.velocity, climbSpeed)
    }

    /**
     * Subclass hook — called after every ascending-section boost. Used by
     * [MinSpeedGenerator] to forgive the "too slow" tick counter while the player is
     * being lifted (they're nearly stationary during a climb).
     */
    protected open fun onAscendingBoost() {}

    protected fun shouldScore(): Boolean {
        return player.player.isGliding && player.position.x - island.blockSpawn.x > 0
    }

    private fun updateBoard(score: Double, time: Instant) {
        player.updateBoard(score, Score.timeFormatter.format(time), seed)
    }

    private fun updateInfo() {
        if (!settings.info) return

        val speed = getSpeed(player)

        player.sendActionBar("<gray>${convertSpeed(speed)}")
    }

    private fun convertSpeed(speed: Double): String {
        return if (settings.metric) {
            "${(speed * 3.6).pretty()} km/h"
        } else {
            "${(speed * 2.236936).pretty()} mph"
        }
    }

    private fun shouldReset(player: ElytraPlayer, pos: Vector): ResetReason? {
        val (idx, section) = sections
            .filter { pos.x < it.value.end.x }
            .minByOrNull { it.key } ?: return null

        val progress = pos.x - section.beginning.x

        if (progress < 0) {
            if (idx == 0 && pos.y < island.blockSpawn.y - settings.radius) {
                IEP.log("Player ${player.name} is below spawn")

                return ResetReason.BOUNDS
            }

            return null
        }

        val isPastSpawn = progress > 0
        val isNotGliding = isPastSpawn && !player.player.isGliding
        val isOutOfBounds = isPastSpawn && !section.isNearPoint(pos, progress.toInt(), settings.radius.toDouble())

        if (isNotGliding) {
            IEP.log("Player ${player.name} is not gliding")
            return ResetReason.FLYING
        } else if (isOutOfBounds) {
            IEP.log("Player ${player.name} is out of bounds")
            return ResetReason.BOUNDS
        }

        return null
    }

    /**
     * Generates the next section.
     */
    protected open fun generate(waitForDisplay: CompletableFuture<Void>) {
        if (sections.isEmpty()) {
            val section = Section(island.blockSpawn.clone().add(Vector(0, pointType.heightOffset, 0)), random)

            sections[0] = section

            IEP.log("Generating section at 0")

            // 15-tick delay for the FIRST section's queue — the upstream-IEP pattern, meant
            // to let the spawn-island chunks reach a freshly joined client before any block
            // change (the client drops a change for a chunk it doesn't have). ClientBlockChanger
            // now holds every block until the client has its chunk, so the delay is no longer
            // needed for that; it only postpones drawing slightly.
            //
            // The BukkitTask handle returned by .run() is stored in [pendingFirstQueue]
            // so reset() can cancel it; otherwise a quick re-teleport (e.g. an instant
            // BOUNDS reset off the edge of the spawn island) would let the orphan task
            // queue a stale block map into a freshly-cleared toChange.
            pendingFirstQueue?.cancel()
            pendingFirstQueue = null
            section.generate(settings, pointType).thenApply { blockMap ->
                waitForDisplay.thenRun {
                    pendingFirstQueue = Task.create(IEP.instance)
                        .delay(15)
                        .execute { blockChanger.queue(blockMap) }
                        .run()
                }
            }

            return
        }

        val latest = sections.maxBy { it.key }
        val idx = latest.key
        val previous = latest.value
        val end = previous.end

        // Bend the pipe UP when the previous section bottomed out near void level.
        // [tick] will firework-boost the player as they enter an ascending section.
        // The threshold (50) is well above the old hard-reset cutoff (25) so the
        // ascending section starts climbing while the player still has plenty of
        // glide altitude — no risk of out-of-bounds on the way up.
        val bias = if (end.y < ASCEND_THRESHOLD_Y) {
            KnotDirector.ASCENDING_VERTICAL_BIAS
        } else {
            KnotDirector.DEFAULT_VERTICAL_BIAS
        }

        val section = Section(end, random, bias)

        sections[idx + 1] = section

        IEP.log("Generating section at ${idx + 1} (ascending=${section.isAscending}, endY was ${end.y})")

        section.generate(settings, pointType).thenApply { blockChanger.queue(it) }
    }

    // resetPlayerHeight was deleted alongside the clone-and-teleport mechanic. It used
    // to warp the player +200 Y to a cloned section when the original ran out of
    // altitude; the teleport killed velocity + gliding state and players reported it
    // as a noticeable stutter. The replacement lives in [applyAscendingBoost] above:
    // when [generate] creates an ascending-bias section, [tick] fires a firework-style
    // velocity boost in the pipe's direction. Result is a smooth, momentum-preserving
    // lift instead of a teleport.

    /**
     * Removes a section from the active set, sending AIR packets to undo its client-side
     * display first. Subclasses override (see [ObstacleGenerator]) to also undo their
     * own client-side overlays.
     */
    open fun clear(idx: Int, section: Section) {
        IEP.log("Clearing section $idx")

        section.clear(player.player, blockChanger)

        sections.remove(idx)
    }

    // Exposed to subclasses in the same package for spoofed-block routing.
    internal fun getBlockChanger(): ClientBlockChanger = blockChanger

    /**
     * Resets the players and knots.
     */
    open fun reset(
        resetReason: ResetReason,
        regenerate: Boolean = true,
        s: Int = settings.seed,
        overrideSeedSettings: Boolean = false
    ) {
        IEP.log("Resetting generator, regenerate = $regenerate, seed = $s")

        if (getScore() > 0.0) {
            val score = Score(
                name = player.name,
                score = getScore(),
                time = getTime().toEpochMilli(),
                seed = seed
            )

            leaderboard.update(player.uuid, score)

            if (settings.fall) {
                Locales.getStringList(player, "reset.lines")
                    .map { line -> updateLine(player, line, score, resetReason) }
                    .forEach { line -> player.send(line) }
            }
        }

        movementScore = 0.0
        start = null
        boostedSectionIdx = Int.MIN_VALUE
        sustainedBoostSection = null
        sustainedBoostEndX = Double.NEGATIVE_INFINITY
        // Cancel any pending 15-tick first-section queue from the PREVIOUS run so
        // it can't push that run's blockMap into our freshly-cleared toChange after
        // the upcoming clear() below.
        try { pendingFirstQueue?.cancel() } catch (_: Throwable) {}
        pendingFirstQueue = null
        if (settings.seed == -1 && !overrideSeedSettings) {
            seed = ThreadLocalRandom.current().nextInt(SEED_BOUND)
            random = Random(seed)
        } else {
            seed = s
            random = Random(s)
        }

        // Iterate over a snapshot so clear() can mutate the underlying map.
        sections.toMap().forEach { clear(it.key, it.value) }
        sections.clear()

        blockChanger.clear()

        rewardHandler.clear()

        if (!regenerate) {
            return
        }

        val spawn = island.playerSpawn.toLocation(World.world)
        spawn.yaw = -90f

        player.player.velocity = Vector(0, 0, 0)
        player.player.fallDistance = 0f

        generate(CompletableFuture.allOf(player.teleport(spawn)))
    }

    private fun updateLine(player: ElytraPlayer, line: String, score: Score, resetReason: ResetReason): String {
        return line.replace("%score%", score.score.pretty())
            .replace("%high-score%", getHighScore().score.pretty())
            .replace("%time%", score.getFormattedTime())
            .replace("%seed%", score.seed.toString())
            .replace("%reason%", Locales.getString(player, "reset.reasons.${resetReason.name.lowercase()}"))
    }

    /**
     * Returns the current speed of the player in m/s.
     * @param player The player to get the speed for.
     * @return The current speed.
     */
    fun getSpeed(player: ElytraPlayer) = player.player.velocity.clone().setY(0).length() * 20

    /**
     * Allows for easy setting of the current [Settings] instance.
     */
    fun set(mapper: (Settings) -> Settings) {
        IEP.log("Updating settings from $settings to ${mapper.invoke(settings)}")

        settings = mapper.invoke(settings)

        player.save(settings)
    }

    companion object {

        const val SEED_BOUND = 1_000_000

        // The pipe bends upward when the previous section ended below this Y.
        // Generous gap above the old hard-reset cutoff (25) so the ascending
        // section starts climbing while the player still has glide altitude
        // — no out-of-bounds risk on the way back up.
        const val ASCEND_THRESHOLD_Y = 50

        // The speed a firework rocket settles at, in blocks per tick (34 blocks a second):
        // vanilla moves the velocity halfway to 1.5 × the look direction and adds 0.1 more each tick.
        const val ROCKET_SPEED = 1.7

        /**
         * The velocity an ascending section gives a player for one tick. Like a firework rocket, it moves
         * [velocity] halfway to [speed] in the direction the player [look]s, so the player steers and the
         * speed settles. It used to add a fixed amount every tick instead, which never settles (elytra drag
         * only takes about 1% a tick): a climb ended at around 7 blocks a tick, four times a rocket's top
         * speed. That push also pointed along the straight line between the section's ends, which the
         * curving pipe strays 20 to 50 blocks from, so players had to fight it.
         *
         * @param look The player's look direction, a unit vector.
         */
        fun climbVelocity(look: Vector, velocity: Vector, speed: Double): Vector {
            val wanted = look.clone().multiply(speed)
            return velocity.clone().add(wanted.subtract(velocity).multiply(0.5))
        }

    }
}
