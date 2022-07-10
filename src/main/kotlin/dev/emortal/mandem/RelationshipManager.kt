package dev.emortal.mandem

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import dev.emortal.mandem.channel.ChatChannel
import dev.emortal.mandem.utils.RedisStorage.redisson
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RelationshipManager {

    val leavingTasks = ConcurrentHashMap<UUID, ScheduledTask>()
    val playerChannelMap = ConcurrentHashMap<UUID, ChatChannel>()
    val partyInviteMap = ConcurrentHashMap<UUID, MutableList<UUID>>()
    val friendRequestMap = ConcurrentHashMap<UUID, MutableList<UUID>>()
    val partyMap = ConcurrentHashMap<UUID, Party>()
    internal val lastMessageMap = ConcurrentHashMap<UUID, UUID>()

    // Replaced by redis!
    //val friendCache = ConcurrentHashMap<UUID, MutableList<UUID>>()

    internal var separator = Component.text(" | ", NamedTextColor.DARK_GRAY)

    internal val partyPrefix = Component.text()
        .append(Component.text("PARTY", TextColor.color(255, 100, 255), TextDecoration.BOLD))
        .append(separator)

    internal val friendPrefix = Component.text()
        .append(Component.text("FRIEND", NamedTextColor.GOLD, TextDecoration.BOLD))
        .append(separator)

    internal val errorColor = NamedTextColor.RED
    internal val errorDark = TextColor.color(200, 0, 0)

    internal val successColor = NamedTextColor.GREEN
    internal val successDark = TextColor.color(0, 200, 0)


    var Player.channel: ChatChannel
        get() = playerChannelMap[uniqueId] ?: ChatChannel.GLOBAL
        set(value) {

            playerChannelMap[uniqueId] = value
        }

    val Player.party
        get() = partyMap[uniqueId]

    suspend fun Player.getFriendsAsync(): MutableList<UUID> {
        val redisFriendList = redisson.getList<UUID>("${uniqueId}friends")
        return redisFriendList.readAll() ?: MandemPlugin.storage!!.getFriendsAsync(uniqueId).also {
            redisFriendList.addAll(it)
        }
    }

    fun Player.inviteToParty(player: Player) {
        if (partyInviteMap[player.uniqueId]?.contains(this.uniqueId) == true) {
            this.sendMessage(Component.text("You have already sent an invite to that player", errorColor))
            return
        }

        partyInviteMap[player.uniqueId]!!.add(this.uniqueId)

        if (!partyMap.contains(this)) Party(this)

        player.sendMessage(
            Component.text()
                .append(partyPrefix)
                .append(Component.text(this.username, TextColor.color(255, 100, 255)))
                .append(
                    Component.text(" has invited you their party! ", TextColor.color(235, 0, 235))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party accept ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Accept invite", NamedTextColor.GREEN)))
                        )
                        .append(Component.space())
                        .append(
                            Component.text("[❌]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/party deny ${this.username}"))
                                .hoverEvent(HoverEvent.showText(Component.text("Deny invite", errorColor)))
                        )
                )
        )

        this.sendMessage(
            Component.text()
                .append(partyPrefix)
                .append(Component.text("Invited '${player.username}' to the party!", NamedTextColor.GREEN))
        )
    }

    fun Player.acceptInvite(player: Player): Boolean {
        if (!partyInviteMap[this.uniqueId]!!.contains(player.uniqueId)) return false

        partyInviteMap[this.uniqueId]!!.remove(player.uniqueId)

        if (player.party == null) return false
        player.party!!.add(this)

        return true
    }

    fun Player.denyInvite(player: Player): Boolean {
        if (!partyInviteMap[this.uniqueId]!!.contains(player.uniqueId)) return false

        partyInviteMap[this.uniqueId]!!.remove(player.uniqueId)

        return true
    }


    // FRIENDS -----

    fun Player.requestFriend(player: Player) = runBlocking {
        if (this@requestFriend.getFriendsAsync().size > 200) {
            this@requestFriend.sendMessage(Component.text("You already have enough friends", errorColor))
            return@runBlocking
        }

        if (friendRequestMap[player.uniqueId]?.contains(this@requestFriend.uniqueId) == true) {
            this@requestFriend.sendMessage(Component.text("You have already sent a request to that player", errorColor))
            return@runBlocking
        }

        if (friendRequestMap[this@requestFriend.uniqueId]?.contains(player.uniqueId) == true) {
            player.acceptFriendRequest(this@requestFriend)
            return@runBlocking
        }
        friendRequestMap[player.uniqueId]!!.add(this@requestFriend.uniqueId)

        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text(this@requestFriend.username, TextColor.color(255, 220, 0), TextDecoration.BOLD))
                .append(
                    Component.text(" wants to be friends! ", TextColor.color(255, 150, 0))
                        .append(
                            Component.text("[✔]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/friend accept ${this@requestFriend.username}"))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        Component.text(
                                            "Accept friend request",
                                            NamedTextColor.GREEN
                                        )
                                    )
                                )
                        )
                        .append(Component.space())
                        .append(
                            Component.text("[❌]", NamedTextColor.RED, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/friend deny ${this@requestFriend.username}"))
                                .hoverEvent(
                                    HoverEvent.showText(
                                        Component.text(
                                            "Deny friend request",
                                            NamedTextColor.RED
                                        )
                                    )
                                )
                        )
                )
        )

        this@requestFriend.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("Sent a friend request to ", successDark))
                .append(Component.text(player.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
    }

    fun Player.acceptFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this.uniqueId]!!.contains(player.uniqueId)) return false

        friendRequestMap[this.uniqueId]!!.remove(player.uniqueId)

        MandemPlugin.storage!!.addFriend(this.uniqueId, player.uniqueId)
        val thisFriendList = redisson.getList<UUID>("${this}friends")
        val uuidFriendList = redisson.getList<UUID>("${player.uniqueId}friends")
        thisFriendList?.add(player.uniqueId)
        uuidFriendList?.add(this.uniqueId)

        this.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with ", successDark))
                .append(Component.text(player.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )
        player.sendMessage(
            Component.text()
                .append(friendPrefix)
                .append(Component.text("You are now friends with ", successDark))
                .append(Component.text(this.username, successColor, TextDecoration.BOLD))
                .append(Component.text("!", successDark))
        )

        return true
    }

    fun UUID.removeFriend(uuid: UUID) {
        MandemPlugin.storage!!.removeFriend(this, uuid)

        val thisFriendList = redisson.getList<UUID>("${this}friends")
        val uuidFriendList = redisson.getList<UUID>("${uuid}friends")
        thisFriendList?.remove(uuid)
        uuidFriendList?.remove(this)
    }

    fun Player.removeFriend(player: Player) = uniqueId.removeFriend(player.uniqueId)

    fun Player.denyFriendRequest(player: Player): Boolean {
        if (!friendRequestMap[this.uniqueId]!!.contains(player.uniqueId)) return false

        friendRequestMap[this.uniqueId]!!.remove(player.uniqueId)

        return true
    }
}