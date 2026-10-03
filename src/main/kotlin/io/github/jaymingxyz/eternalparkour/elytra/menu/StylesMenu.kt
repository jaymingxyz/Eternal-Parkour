package io.github.jaymingxyz.eternalparkour.elytra.menu

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.ResetReason
import io.github.jaymingxyz.eternalparkour.elytra.generator.Settings
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer
import io.github.jaymingxyz.eternalparkour.elytra.style.RandomStyle
import io.github.jaymingxyz.eternalparkour.elytra.style.Style
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle
import org.bukkit.Material

object StylesMenu {

    fun open(player: ElytraPlayer) {
        // a paged list: there are more styles than fit next to the buttons of a 3-row menu
        val menu = MenuStyle.list(Locales.getString(player, "styles.title"),
            Locales.getItem(player, "previous page"),
            Locales.getItem(player, "next page"),
            Locales.getItem(player, "empty"))
        val bar = MenuStyle.center(menu)

        val styles = IEP.getStyles()
        val generator = player.getGenerator()
        val items = mutableListOf<MenuItem>()

        for (style in styles) {
            if (!player.hasPermission("iep.setting.style.${style.name()}")) {
                continue
            }

            // Resolve the localized display name from locales/<lang>.yml under
            // styles.names.<key>; fall back to the raw config.yml key if the entry
            // is missing so we don't render "<bold></bold>" for an unmapped style.
            val key = style.name()
            val localizedName = Locales.getString(player, "styles.names.$key").ifBlank { key }
            val typeKey = if (style is RandomStyle) "random" else "incremental"
            val localizedType = Locales.getString(player, "styles.types.$typeKey").ifBlank { typeKey }
            val current = generator.settings.style == key

            val item = Locales.getItem(player, "styles.style", localizedName, localizedType)
                .glowing(current)
                .material(icon(style))

            items += MenuStyle.hint(item, Locales.getString(player, if (current) "hints.selected" else "hints.select"))
                .click({
                    generator.set { settings -> Settings(settings, style = key) }
                    // todo for speed demon
                    if (generator.getScore() == 0.0) {
                        generator.reset(ResetReason.RESET)
                    }
                    player.player.closeInventory()
                })
        }

        menu.addToDisplay(items)
            .item(bar - 2, MenuStyle.hint(Locales.getItem(player, "styles.random").material(icon(styles.random())),
                Locales.getString(player, "hints.select"))
                .click({
                    generator.set { settings -> Settings(settings, style = styles.random().name()) }
                    player.player.closeInventory()
                }))
            .item(bar, Locales.getItem(player, "go back").click({ SettingsMenu.open(player) }))
            .open(player.player)
    }

    // A block of the style to show it with. Spiral styles are mostly air, and some blocks have no item.
    private fun icon(style: Style): Material {
        repeat(64) {
            val material = style.next()
            if (material.isItem && !material.isAir) {
                return material
            }
        }
        return Material.STONE
    }
}
