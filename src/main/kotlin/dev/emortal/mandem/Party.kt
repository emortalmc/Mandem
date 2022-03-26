package dev.emortal.mandem

import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.utils.armify
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.concurrent.ConcurrentHashMap

class Party(var leader: Player) {

    val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    val playerAudience = Audience.audience(players)
    var privateGames = false

    init {
        add(leader)
        leader.sendMessage(
            Component.text("Welcome to your party, ", NamedTextColor.GRAY)
                .append(Component.text(leader.username, NamedTextColor.GREEN, TextDecoration.BOLD))
                .armify()
        )

        /*Manager.globalEvent.listenOnly<PlayerJoinGameEvent> {
            val game = getGame()
            players.forEach {
                it.joinGame(game)
            }
        }*/
    }

    fun add(player: Player, sendMessage: Boolean = true) {
        players.add(player)
        RelationshipManager.partyMap[player.uniqueId] = this
        if (player != leader && sendMessage) playerAudience.sendMessage(
            Component.text()
                .append(Component.text("JOIN PARTY", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the party", NamedTextColor.GRAY))
        )
    }

    fun remove(player: Player, sendMessage: Boolean = true) {
        if (sendMessage) playerAudience.sendMessage(
            Component.text()
                .append(Component.text("QUIT PARTY", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.RED))
                .append(Component.text(" left the party", NamedTextColor.GRAY))
        )

        players.remove(player)
        if (players.size == 1) leader = players.first()

        RelationshipManager.partyMap.remove(player.uniqueId)
    }

    fun destroy() {
        playerAudience.sendMessage(Component.text("The party was destroyed", NamedTextColor.RED))
        players.forEach {
            RelationshipManager.partyMap.remove(it.uniqueId)
        }
        players.clear()
    }

    //override fun getPlayers(): MutableCollection<Player> = players

}