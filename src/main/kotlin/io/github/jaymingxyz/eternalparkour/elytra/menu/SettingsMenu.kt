package io.github.jaymingxyz.eternalparkour.elytra.menu

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.ResetReason
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings.Companion.asStyle
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.SliderItem
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task

object SettingsMenu {

    fun open(player: ElytraPlayer) {
        val menu = MenuStyle.frame(Menu(4, Locales.getString(player, "settings.title")))
            .distributeRowsEvenly()
        val generator = player.getGenerator()
        val settings = generator.settings

        if (canSee(player, "style")) {
            val style = settings.style.asStyle()

            menu.item(
                9,
                Locales.getItem(player, "settings.styles", style.name())
                    .material(style.next())
                    .click({ StylesMenu.open(player) })
            )
        }

        if (canSee(player, "radius")) {
            val item = Locales.getItem(player, "settings.radius", settings.radius.toString())

            menu.item(
                10, SliderItem()
                    .initial(generator.settings.radius - 3)
                    .add(0, item) {
                        generator.set { settings -> Settings(settings, radius = 3) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(1, item) {
                        generator.set { settings -> Settings(settings, radius = 4) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(2, item) {
                        generator.set { settings -> Settings(settings, radius = 5) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(3, item) {
                        generator.set { settings -> Settings(settings, radius = 6) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
            )
        }

        if (canSee(player, "time")) {
            val item = Locales.getItem(player, "settings.time", settings.time.toString())

            menu.item(
                11, SliderItem()
                    .initial(settings.time / 6000)
                    .add(0, item) {
                        generator.set { settings -> Settings(settings, time = 0) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(1, item) {
                        generator.set { settings -> Settings(settings, time = 6000) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(2, item) {
                        generator.set { settings -> Settings(settings, time = 12000) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
                    .add(3, item) {
                        generator.set { settings -> Settings(settings, time = 18000) }
                        Task.create(IEP.instance).delay(1).execute { open(player) }.run()
                        return@add true
                    }
            )
        }

        if (canSee(player, "seed")) {
            val seed = if (settings.seed == -1) Locales.getString(player, "settings.seed.random") else generator.seed.toString()

            menu.item(12, Locales.getItem(player, "settings.seed", seed)
                .click({
                    generator.set { settings -> Settings(settings, seed = -1) }
                    generator.reset(ResetReason.RESET)
                    open(player)
                }))
        }

        if (canSee(player, "locale")) {
            val locales = Locales.getLocales().toList()
            val item = SliderItem()
                .initial(locales.indexOf(settings.locale))

            repeat(locales.size) {
                val locale = locales[it]

                item.add(
                    it, Locales.getItem(player, "settings.locale", locale)
                ) { _ ->
                    generator.set { settings -> Settings(settings, locale = locale) }

                    Task.create(IEP.instance).delay(1).execute {
                        if (settings.locale != locale) {
                            open(player)
                        }
                    }.run()

                    return@add true
                }
            }

            menu.item(13, item)
        }

        if (canSee(player, "fall")) {
            menu.item(
                19,
                getBooleanItem(player, "settings.fall", settings.fall)
                    .click({
                        generator.set { settings -> Settings(settings, fall = !settings.fall) }
                        open(player)
                    }))
        }

        if (canSee(player, "info")) {
            menu.item(
                20,
                getBooleanItem(player, "settings.info", settings.info)
                    .click({
                        generator.set { settings -> Settings(settings, info = !settings.info) }
                        open(player)
                    }))
        }


        if (canSee(player, "metric")) {
            menu.item(
                21,
                getBooleanItem(player, "settings.metric", settings.metric)
                    .click({
                        generator.set { settings -> Settings(settings, metric = !settings.metric) }
                        open(player)
                    }))
        }

        menu.item(31, Locales.getItem(player, "close").click({ player.player.closeInventory() }))
            .open(player.player)
    }

    private fun canSee(player: ElytraPlayer, setting: String): Boolean {
        return Config.CONFIG.getBoolean("settings.$setting.enabled") &&
            player.hasPermission("iep.setting.$setting")
    }

    private fun getBooleanItem(player: ElytraPlayer, path: String, boolean: Boolean): Item {
        val base = if (boolean) {
            Locales.getItem(player, "settings.enabled")
        } else {
            Locales.getItem(player, "settings.disabled")
        }
        val item = Locales.getItem(player, path, base.lore[0])

        base.name(base.name + item.name)
        base.lore(item.lore)

        return base
    }
}