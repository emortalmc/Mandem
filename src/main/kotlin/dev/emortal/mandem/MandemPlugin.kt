package dev.emortal.mandem

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import dev.emortal.mandem.commands.ShrugCommand
import dev.emortal.mandem.config.ConfigHelper
import dev.emortal.mandem.config.MandemConfig
import dev.emortal.mandem.db.MySQLStorage
import dev.emortal.mandem.db.Storage
import dev.emortal.mandem.utils.RedisStorage.redisson
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import java.nio.file.Path
import java.util.logging.Logger

@Plugin(
    id = "mandem",
    name = "Mandem",
    version = "1.0.0",
    description = "Wait, who's mandem?",
    dependencies = [Dependency(id = "luckperms"), Dependency(id = "datadependency")]
)
class MandemPlugin @Inject constructor(private val server: ProxyServer, private val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        mandemConfig = ConfigHelper.initConfigFile(configPath, MandemConfig())
        plugin = this
        luckperms = LuckPermsProvider.get()

        if (mandemConfig.enabled) {
            storage = MySQLStorage()
        }

        Companion.server = server

        server.eventManager.register(this, EventListener(this))

        ShrugCommand.register()
        //PartyCommand.register()
        //ReplyCommand.register()
        //ChannelCommand.register()

        logger.info("[Mandem] has arrived!")

    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        redisson.shutdown()
    }

    companion object {
        lateinit var plugin: MandemPlugin
        lateinit var server: ProxyServer
        lateinit var luckperms: LuckPerms

        var storage: Storage? = null

        lateinit var mandemConfig: MandemConfig
        val configPath = Path.of("./mandemconfig.json")
    }

}