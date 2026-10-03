package io.github.jaymingxyz.eternalparkour.elytra

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.jaymingxyz.eternalparkour.core.EternalParkour
import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.MinSpeedGenerator
import io.github.jaymingxyz.eternalparkour.elytra.generator.TimeTrialGenerator
import io.github.jaymingxyz.eternalparkour.elytra.hook.PapiHook
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score
import io.github.jaymingxyz.eternalparkour.elytra.mode.*
import io.github.jaymingxyz.eternalparkour.elytra.reward.Rewards
import io.github.jaymingxyz.eternalparkour.elytra.style.IncrementalStyle
import io.github.jaymingxyz.eternalparkour.elytra.style.RandomStyle
import io.github.jaymingxyz.eternalparkour.elytra.style.Style
import io.github.jaymingxyz.eternalparkour.elytra.world.Divider
import io.github.jaymingxyz.eternalparkour.elytra.world.World
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematics
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Logging
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task
import org.bukkit.Material
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

object IEP {

    lateinit var instance: EternalParkour
        private set

    val dataFolder: File
        get() = instance.dataFolder.resolve("elytra")

    val logging: Logging
        get() = EternalParkour.logging()

    fun enable(plugin: EternalParkour) {
        instance = plugin
        stopping = false
        modes.clear()
        styles.clear()

        plugin.registerListener(Events)
        plugin.registerCommand("iep", Command)

        saveFile("schematics/spawn-island")

        World.create()
        Locales.init()
        Files.list(dataFolder.toPath().resolve("schematics")).use { files ->
            Schematics.addFromFiles(plugin, *files.map { it.toFile() }.toList().toTypedArray())
        }

        loadStyles()

        registerMode(DefaultMode, false)
        registerMode(SpeedDemonMode)
        registerMode(MinSpeedMode)
        registerMode(TimeTrialMode)
        registerMode(CloseMode)
        registerMode(ObstacleMode)

        if (plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            log("Registered PlaceholderAPI Hook")
            papiHook = PapiHook
            PapiHook.register()
        }
        if (plugin.server.pluginManager.isPluginEnabled("Vault")) {
            log("Registered Vault Hook")
        }
        registerProxyChannel()

        Task.create(plugin)
            .async()
            .repeat(5 * 60 * 20)
            .delay(5 * 60 * 20)
            .execute {
                log("Saving all leaderboards")
                modes.forEach { it.leaderboard.save() }
            }
            .run()

    }

    /**
     * Reads the elytra config, rewards and locale files again. Called by /ep reload.
     *
     * Settings that are read while playing apply right away, including to running courses. Turning
     * modes on or off, MySQL and the world still need a restart.
     */
    fun reload() {
        Config.entries.forEach { it.load() }

        Locales.init()
        loadStyles()
        Rewards.load()
        MinSpeedGenerator.load()
        TimeTrialGenerator.load()
        Score.load()

        // an `is` check, so a disabled time trial mode isn't created (and its leaderboard loaded) here
        modes.filterIsInstance<TimeTrialMode>().forEach { it.leaderboard.minScore = TimeTrialGenerator.SCORE }

        registerProxyChannel()

        log("Reloaded all config files")
    }

    private fun registerProxyChannel() {
        val messenger = instance.server.messenger

        if (Config.CONFIG.getBoolean("proxy.enabled") && !messenger.isOutgoingChannelRegistered(instance, "BungeeCord")) {
            log("Registered BungeeCord Hook")
            messenger.registerOutgoingPluginChannel(instance, "BungeeCord")
        }
    }

    fun saveFile(path: String) {
        val file = dataFolder.resolve(path)

        if (!file.exists()) {
            instance.saveResource("elytra/$path", false)
        }
    }

    // Replaces the styles as a whole: a running course looks its style up by name every tick, and one
    // that was removed falls back to the first style.
    private fun loadStyles() {
        styles.clear()

        registerStyle("styles.random") { name, data -> RandomStyle(name, data) }
        registerStyle("styles.incremental") { name, data -> IncrementalStyle(name, data) }
    }

    private fun registerStyle(path: String, fn: (name: String, data: List<Material>) -> Style) {
        Config.CONFIG.getPaths(path).forEach { name ->
            registerStyle(
                fn.invoke(name, Config.CONFIG.getStringList("$path.$name")
                    .map {
                        try {
                            return@map Material.getMaterial(it.uppercase())!!
                        } catch (_: NullPointerException) {
                            logging.error("Invalid material in style $path.$name: $it")
                            return@map Material.STONE
                        }
                    })
            )
        }
    }

    fun disable() {
        stopping = true

        try {
            papiHook?.unregister()
            papiHook = null

            for (generator in HashSet(Divider.generators)) {
                generator.player.leave(urgent = true)
            }

            getModes().forEach { it.leaderboard.save() }

            World.delete()
        } catch (_: Exception) {
            // for no class found errors if nobody has joined yet
        }
    }

    var stopping = false
        private set
    var papiHook: PapiHook? = null

    val GSON: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun log(message: String) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] $message")
        }
    }

    private val modes: MutableList<Mode> = mutableListOf()

    fun registerMode(mode: Mode, checkExists: Boolean = true) {
        if (!Config.CONFIG.getBoolean("mode-settings.${mode.name.replace(" ", "-")}.enabled") && checkExists) {
            return
        }

        log("Registered mode ${mode.name}")

        modes += mode
    }

    fun getMode(name: String): Mode? = modes.firstOrNull { it.name == name }

    fun getModes() = modes.toList()

    private val styles: MutableList<Style> = mutableListOf()

    fun registerStyle(style: Style) {
        log("Registered style ${style.name()}")

        styles += style
    }

    fun getStyle(name: String) = styles.firstOrNull { it.name() == name } ?: styles.first()

    fun getStyles() = styles.toList()
}
