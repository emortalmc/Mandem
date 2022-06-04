package dev.emortal.mandem.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.MandemPlugin.Companion.server
import dev.emortal.mandem.utils.PermissionUtils.displayName
import net.kyori.adventure.text.Component


object ShrugCommand : MandemCommand() {

    override fun createBrigadierCommand(): LiteralCommandNode<CommandSource> {

        val playNode = LiteralArgumentBuilder.literal<CommandSource>("shrug")
            .executes {
                val player = it.source as? Player
                if (player == null) {
                    it.source.sendMessage(Component.text("¯\\_☻_/¯"))
                    return@executes 1
                }

                server.allPlayers.forEach {
                    it.sendMessage(
                        Component.text()
                            .append(player.displayName)
                            .append(Component.text(": ¯\\_☻_/¯"))
                            .build()
                    )
                }

                1
            }
            .build()

        return playNode

    }

}