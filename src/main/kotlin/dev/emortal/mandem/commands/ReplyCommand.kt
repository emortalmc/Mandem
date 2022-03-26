package dev.emortal.mandem.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.MandemPlugin.Companion.server
import dev.emortal.mandem.RelationshipManager
import dev.emortal.mandem.RelationshipManager.getFriendsAsync
import dev.emortal.mandem.channel.ChatChannel
import dev.emortal.mandem.utils.PermissionUtils.displayName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

object ReplyCommand : MandemCommand() {

    override fun createBrigadierCommand(): LiteralCommandNode<CommandSource> {

        val channelNode = LiteralArgumentBuilder.literal<CommandSource>("reply")
            .executes {
                val player = it.source as? Player ?: return@executes 1
                player.sendMessage(Component.text("Usage: /reply <message>", NamedTextColor.RED))
                1
            }
            .build()

        val channelArgNode = RequiredArgumentBuilder
            .argument<CommandSource, String>("message", StringArgumentType.greedyString())
            .suggests { ctx: CommandContext<CommandSource>, builder: SuggestionsBuilder ->
                ChatChannel.values().forEach {
                    builder.suggest(it.name)
                }
                return@suggests builder.buildFuture()
            }
            .executes {
                CoroutineScope(Dispatchers.IO).launch {
                    val player = it.source as? Player
                    if (player == null) {
                        it.source.sendMessage(Component.text("Reply command cannot be used via console", NamedTextColor.RED))
                        return@launch
                    }

                    val lastMessagePlayer = RelationshipManager.lastMessageMap[player.uniqueId]?.let { server.getPlayer(it) }
                    if (lastMessagePlayer == null || lastMessagePlayer.isEmpty) {
                        player.sendMessage(Component.text("That player is not online", RelationshipManager.errorDark))
                        return@launch
                    }
                    val replyingPlayer = lastMessagePlayer.get()

                    val message = (it.arguments["message"]?.result as? String)?.uppercase() ?: return@launch
                    if (!player.getFriendsAsync().contains(replyingPlayer.uniqueId)) {
                        player.sendMessage(
                            Component.text()
                                .append(Component.text("You are not friends with ", RelationshipManager.errorDark))
                                .append(Component.text(replyingPlayer.username, RelationshipManager.errorColor, TextDecoration.BOLD))

                        )
                        return@launch
                    }

                    val meComponent = Component.text("ME", NamedTextColor.AQUA, TextDecoration.BOLD)
                    val arrowComponent = Component.text(" → ", NamedTextColor.GRAY)
                    val messageComponent = Component.text(message)

                    replyingPlayer.sendMessage(
                        Component.text()
                            .append(Component.text("[", NamedTextColor.DARK_AQUA))
                            .append(player.displayName)
                            .append(arrowComponent)
                            .append(meComponent)
                            .append(Component.text("] ", NamedTextColor.DARK_AQUA))
                            .append(messageComponent)
                    )
                    player.sendMessage(
                        Component.text()
                            .append(Component.text("[", NamedTextColor.DARK_AQUA))
                            .append(meComponent)
                            .append(arrowComponent)
                            .append(replyingPlayer.displayName)
                            .append(Component.text("] ", NamedTextColor.DARK_AQUA))
                            .append(messageComponent)
                    )

                    replyingPlayer.playSound(Sound.sound(Key.key("entity.item.pickup"), Sound.Source.MASTER, 1f, 1f))
                }
                1
            }
            .build()

        channelNode.addChild(channelArgNode)

        return channelNode

    }

}