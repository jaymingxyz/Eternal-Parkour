package io.github.jaymingxyz.eternalparkour.elytra.storage

import com.google.gson.reflect.TypeToken
import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal object DiskStorage {

    private val SCORES_TYPE = object : TypeToken<Map<UUID, Score>>() {}.type

    fun init(uuid: UUID) {
        val file = getPlayerFile(uuid)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun init(leaderboard: Leaderboard) {
        val file = getLeaderboardFile(leaderboard.name)

        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun save(uuid: UUID, settings: Settings) {
        writeAtomically(getPlayerFile(uuid), IEP.GSON.toJson(settings))
    }

    fun load(uuid: UUID): Settings? {
        return try {
            getPlayerFile(uuid).reader(StandardCharsets.UTF_8).use { IEP.GSON.fromJson(it, Settings::class.java) }
        } catch (ex: Exception) {
            // a corrupt file must not stop the player from playing; they get default settings
            IEP.logging.stack("Error while reading elytra settings of $uuid, using default settings", ex)
            null
        }
    }

    fun save(leaderboard: Leaderboard) {
        writeAtomically(getLeaderboardFile(leaderboard.name), IEP.GSON.toJson(HashMap(leaderboard.data), SCORES_TYPE))
    }

    fun load(leaderboard: Leaderboard) {
        val scores: Map<UUID, Score> = try {
            getLeaderboardFile(leaderboard.name).reader(StandardCharsets.UTF_8).use { IEP.GSON.fromJson(it, SCORES_TYPE) }
                ?: return
        } catch (ex: Exception) {
            IEP.logging.stack("Error while reading elytra leaderboard ${leaderboard.name}", ex)
            return
        }

        leaderboard.data.putAll(scores)
    }

    private fun writeAtomically(file: File, json: String) {
        file.parentFile.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")

        Files.writeString(temp.toPath(), json, StandardCharsets.UTF_8)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun getPlayerFile(uuid: UUID) = File(IEP.dataFolder, "players/$uuid.json")
    private fun getLeaderboardFile(name: String) = File(IEP.dataFolder, "leaderboards/$name.json")
}
