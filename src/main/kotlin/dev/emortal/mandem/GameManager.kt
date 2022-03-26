package dev.emortal.mandem

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.ServerInfo
import dev.emortal.mandem.MandemPlugin.Companion.server
import dev.emortal.mandem.utils.RedisStorage.redisson
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object GameManager {
    val logger = LoggerFactory.getLogger("GameManager")

    private val localhostName = server.allServers.first().serverInfo.address.hostName

    val serverGameMap = ConcurrentHashMap<String, String>()

    fun initListener() {


        // Subscribe to registergame channel
        redisson.getTopic("registergame").addListenerAsync(String::class.java) { channel, msg ->
            val args = msg.split(" ")
            val gameName = args[0].lowercase()
            val serverName = args[1].lowercase()
            val serverPort = args[2].toInt()

            logger.info("Registering new server")

            if (!server.getServer(serverName).isPresent && server.allServers.any { it.serverInfo.address.port == serverPort }) {
                logger.error("Port already in use")
                return@addListenerAsync
            }

            logger.info("Game: ${gameName}")
            logger.info("Server: ${serverName}")
            logger.info("Port: ${serverPort}")

            serverGameMap[gameName] = serverName

            if (!server.getServer(serverName).isPresent) {
                server.registerServer(ServerInfo(serverName, InetSocketAddress(localhostName, serverPort)))
            }
        }

    }

    internal fun Player.sendToServer(serverName: String, game: String) {
        if (!serverGameMap.containsKey(game)) {
            logger.error("Game type not registered")
            return
        }

        var foundServer = false
        currentServer.ifPresent {
            if (it.serverInfo.name == serverName) {
                foundServer = true
                // Player is already connected to this server, instead publish redis changegame message
                logger.info("Player already on correct server, sending changegame ${serverName} ${game}")
                redisson.getTopic("playerpubsub${serverName}").publish("changegame $uniqueId $game")
                return@ifPresent
            }
        }
        if (foundServer) return


        server.getServer(serverName).ifPresentOrElse({ server ->
            logger.info("${this.username} joining server ${server}, subgame: ${game}")
            redisson.getBucket<String>("${this.uniqueId}-subgame").trySetAsync(game, 15, TimeUnit.SECONDS)

            val future = this.createConnectionRequest(server).connectWithIndication()

            future.thenAccept { successful ->
                if (successful) {
                    logger.info("Sent player ${this.username} to server ${serverName}")
                }
            }
        }, {
            logger.error("Couldn't get server by the name of ${serverName}, did it go offline?")

            this.sendMessage(Component.text("Failed to join game", NamedTextColor.RED))
        })
    }

}