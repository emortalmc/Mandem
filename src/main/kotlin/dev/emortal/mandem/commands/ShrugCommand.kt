package dev.emortal.mandem.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
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
                player.spoofChatInput("¯\\_☻_/¯")
                1
            }
            .build()

        return playNode

    }

}