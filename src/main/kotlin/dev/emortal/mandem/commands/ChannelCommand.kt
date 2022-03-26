package dev.emortal.mandem.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.RelationshipManager.channel
import dev.emortal.mandem.channel.ChatChannel
import dev.emortal.mandem.utils.enumValueOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor


object ChannelCommand : MandemCommand() {

    override fun createBrigadierCommand(): LiteralCommandNode<CommandSource> {

        val channelNode = LiteralArgumentBuilder.literal<CommandSource>("channel")
            .executes {
                val player = it.source as? Player ?: return@executes 1
                player.sendMessage(Component.text("You are in channel ${player.channel.name}", NamedTextColor.GOLD))
                1
            }
            .build()

        val channelArgNode = RequiredArgumentBuilder
            .argument<CommandSource, String>("channel", StringArgumentType.string())
            .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                ChatChannel.values().forEach {
                    builder.suggest(it.name)
                }
                return@suggests builder.buildFuture()
            }
            .executes {
                val player = it.source as? Player
                if (player == null) {
                    it.source.sendMessage(Component.text("Channel command cannot be used via console", NamedTextColor.RED))
                    return@executes 0
                }

                val channel = (it.arguments["channel"]?.result as? String)?.uppercase() ?: return@executes 0

                val channelEnum = enumValueOrNull<ChatChannel>(channel)
                if (channelEnum == null) {
                    it.source.sendMessage(Component.text("Invalid channel", NamedTextColor.RED))
                    return@executes 0
                }

                player.channel = channelEnum
                it.source.sendMessage(Component.text("Set channel to $channelEnum", NamedTextColor.GOLD))

                1
            }
            .build()

        channelNode.addChild(channelArgNode)

        return channelNode

    }

}