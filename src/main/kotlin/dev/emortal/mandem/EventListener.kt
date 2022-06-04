package dev.emortal.mandem

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.MandemPlugin.Companion.mandemConfig
import dev.emortal.mandem.MandemPlugin.Companion.server
import dev.emortal.mandem.MandemPlugin.Companion.storage
import dev.emortal.mandem.RelationshipManager.channel
import dev.emortal.mandem.RelationshipManager.friendPrefix
import dev.emortal.mandem.RelationshipManager.getFriendsAsync
import dev.emortal.mandem.RelationshipManager.leavingTasks
import dev.emortal.mandem.RelationshipManager.party
import dev.emortal.mandem.RelationshipManager.partyPrefix
import dev.emortal.mandem.channel.ChatChannel
import dev.emortal.mandem.utils.PermissionUtils.displayName
import dev.emortal.mandem.utils.RedisStorage.redisson
import dev.emortal.mandem.utils.plainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.LoggerFactory
import java.time.Duration

class EventListener(val plugin: MandemPlugin) {

    val logger = LoggerFactory.getLogger("EventListener")
    val chatLogger = LoggerFactory.getLogger("Chat")
    val commandLogger = LoggerFactory.getLogger("Command")
    val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun playerJoin(e: LoginEvent) {
        if (mandemConfig.enabled) {
            CoroutineScope(Dispatchers.IO).launch {
                Audience.audience(e.player.getFriendsAsync().mapNotNull { server.getPlayer(it).orElseGet { null } })
                    .sendMessage(
                        Component.text()
                            .append(friendPrefix)
                            .append(Component.text(e.player.username, NamedTextColor.GREEN))
                            .append(Component.text(" joined the server", NamedTextColor.GRAY))
                    )
            }
        }

        leavingTasks[e.player.uniqueId]?.cancel()
        leavingTasks.remove(e.player.uniqueId)

        val cachedUsername = redisson.getBucket<String>("${e.player.uniqueId}username")
        logger.info(cachedUsername.get())

        RelationshipManager.partyInviteMap[e.player.uniqueId] = mutableListOf()
        RelationshipManager.friendRequestMap[e.player.uniqueId] = mutableListOf()

        // If cache is outdated, re-set
        // May be better to use SQL instead of redis for this... :P
        storage?.setCachedUsername(e.player.uniqueId, e.player.username)
        if (cachedUsername.get() != e.player.username) {
            cachedUsername.set(e.player.username)

        }
    }

    @Subscribe
    fun playerLeave(e: DisconnectEvent) {
        val player = e.player

        RelationshipManager.partyInviteMap.remove(player.uniqueId)
        RelationshipManager.friendRequestMap.remove(player.uniqueId)

        if (mandemConfig.enabled) {
            CoroutineScope(Dispatchers.IO).launch {
                Audience.audience(storage!!.getFriendsAsync(e.player.uniqueId).mapNotNull { server.getPlayer(it).orElseGet { null } })
                    .sendMessage(
                        Component.text()
                            .append(friendPrefix)
                            .append(Component.text(player.username, NamedTextColor.RED))
                            .append(Component.text(" left the server", NamedTextColor.GRAY))
                    )
            }
        }

        RelationshipManager.partyInviteMap.remove(player.uniqueId)

        if (player.party != null) leavingTasks[player.uniqueId] =
            server.scheduler.buildTask(plugin) {
                player.party?.playerAudience?.sendMessage(
                    Component.text(
                        "${player.username} was kicked from the party because they were offline",
                        NamedTextColor.GOLD
                    )
                )
                player.party?.remove(player, false)
            }.delay(Duration.ofMinutes(5)).schedule()
    }

    @Subscribe
    fun playerJoinServer(e: ServerConnectedEvent) {

    }

    @Subscribe
    fun onCommand(e: CommandExecuteEvent) {
        if (e.commandSource !is Player) return
        val username = (e.commandSource as? Player)?.username
        commandLogger.info("${username} ran command: ${e.command}")
    }

    @Subscribe(order = PostOrder.LAST)
    fun onChat(e: PlayerChatEvent) {
        if (e.result == PlayerChatEvent.ChatResult.denied()) return

        val player = e.player
        e.result = PlayerChatEvent.ChatResult.denied()

        val replacedMessage = e.message
            .replace("<br>", "", true)
            .replace("\uF801", "", true)
            .replace("\uF802", "", true)
            .replace("\uF803", "", true)
            .replace("\uF804", "", true)
            .replace("\uF805", "", true)
            .replace("\uF806", "", true)
            .replace("\uF807", "", true)
            .replace("\uF808", "", true)
            .replace("\uE006", "", true)
            .replace("\uE008", "", true)
            .replace("\uE009", "", true)
            .replace("\uE010", "", true)
            .replace("\uE00A", "", true)
            .replace("\uE00C", "", true)
            .replace("\uE00D", "", true)
            .replace("\uE00E", "", true)
            .replace("\uE00F", "", true)
            .replace("\uE011", "", true)

        if (replacedMessage.isBlank()) return

        val message = if (player.hasPermission("chat.colors")) {
            try {
                miniMessage.deserialize(replacedMessage)
            } catch (ignored: Exception) {
                Component.text(replacedMessage)
            }
        } else {
            Component.text(replacedMessage)
        }

        chatLogger.info("${player.displayName.plainText()}: ${replacedMessage}")

        if (player.channel == ChatChannel.PARTY) {
            if (player.party == null) {
                player.sendMessage(
                    Component.text(
                        "Set your channel to global because you are not in a party",
                        NamedTextColor.GOLD
                    )
                )

                player.channel = ChatChannel.GLOBAL
                return
            }

            player.party!!.playerAudience.sendMessage(
                Component.text()
                    .append(partyPrefix)
                    .append(player.displayName)
                    .append(Component.text(": "))
                    .append(message)
                    .build()
            )
            return
        }



        server.allPlayers.forEach {
            it.sendMessage(
                Component.text()
                    .append(player.displayName)
                    .append(Component.text(": "))
                    .append(message)
                    .build()
            )
        }

    }

}