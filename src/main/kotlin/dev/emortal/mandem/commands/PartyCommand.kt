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
import dev.emortal.mandem.RelationshipManager.errorColor
import dev.emortal.mandem.RelationshipManager.getFriendsAsync
import dev.emortal.mandem.RelationshipManager.inviteToParty
import dev.emortal.mandem.RelationshipManager.party
import dev.emortal.mandem.channel.ChatChannel
import dev.emortal.mandem.utils.PermissionUtils.displayName
import dev.emortal.mandem.utils.armify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

object PartyCommand : MandemCommand() {

    override fun createBrigadierCommand(): LiteralCommandNode<CommandSource> {

        val partyNode = LiteralArgumentBuilder.literal<CommandSource>("party")
            .executes {
                val player = it.source as? Player ?: return@executes 1

                if (player.party == null) {
                    player.sendMessage(
                        Component.text(
                            "Invite others to your party with /party <username>",
                            NamedTextColor.LIGHT_PURPLE
                        )
                    )
                } else {
                    player.spoofChatInput("/party list")
                }

                1
            }
            .build()

        val userArg = RequiredArgumentBuilder
            .argument<CommandSource, String>("user", StringArgumentType.string())
            .executes {
                /*val player = it.source as? Player ?: return@executes 1

                val userInvite = (it.arguments["user"]?.result as? String) ?: return@executes 0
                val user = server.getPlayer(userInvite).orElseGet { null }

                if (user == null) {
                    player.sendMessage(Component.text("That player is not online", errorColor))
                    return@executes 1
                }

                if (user.uniqueId == player.uniqueId) {
                    player.sendMessage(Component.text("Are you really that lonely?", errorColor))
                    return@executes 1
                }

                val amountOfInvites = RelationshipManager.partyInviteMap[player.uniqueId]?.size ?: 0

                if (amountOfInvites > 4) {
                    player.sendMessage(Component.text("You are sending too many invites", errorColor))
                    return@executes 1
                }

                player.inviteToParty(user)*/



                it.source.sendMessage(Component.text("user ${it.arguments.size}"))

                1
            }
            .build()



        val listNode = LiteralArgumentBuilder.literal<CommandSource>("list")
            .executes {
                val player = it.source as? Player ?: return@executes 1

                if (player.party == null) {
                    player.sendMessage(Component.text("You are not in a party", errorColor))
                    return@executes 1
                }

                val message = Component.text()
                    .append(Component.text("Players in party:", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                    .append(Component.text(" ${player.party!!.players.size}\n", NamedTextColor.DARK_GRAY))

                player.party!!.players.forEach {
                    message.append(Component.text("\n - ", NamedTextColor.DARK_GRAY))
                    message.append(Component.text(it.username, NamedTextColor.GRAY))
                }

                player.sendMessage(message.armify())

                1
            }
            .build()
        partyNode.addChild(listNode)

        val testNode = LiteralArgumentBuilder.literal<CommandSource>("test")
            .executes {
                it.source.sendMessage(Component.text("Test"))
                1
            }
            .build()
        partyNode.addChild(testNode)
        testNode.addChild(userArg)

        val destroyNode = LiteralArgumentBuilder.literal<CommandSource>("destroy")
            .executes {
                val player = it.source as? Player ?: return@executes 1

                if (player.party == null) {
                    player.sendMessage(Component.text("You are not in a party", errorColor))
                    return@executes 1
                }
                if (player.party!!.leader != player) {
                    player.sendMessage(Component.text("You are not the party leader", errorColor))
                    return@executes 1
                }

                player.party?.destroy()

                1
            }
            .build()
        partyNode.addChild(destroyNode)

        val leaveNode = LiteralArgumentBuilder.literal<CommandSource>("leave")
            .executes {
                val player = it.source as? Player ?: return@executes 1

                if (player.party == null) {
                    player.sendMessage(Component.text("You are not in a party", errorColor))
                    return@executes 1
                }

                player.party?.remove(player)

                1
            }
            .build()
        partyNode.addChild(leaveNode)


        return partyNode

    }

}