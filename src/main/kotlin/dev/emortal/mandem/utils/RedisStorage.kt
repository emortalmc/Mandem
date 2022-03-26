package dev.emortal.mandem.utils

import dev.emortal.mandem.MandemPlugin
import org.redisson.Redisson
import org.redisson.config.Config

object RedisStorage {

    val redisson = Redisson.create(Config().also { it.useSingleServer().setAddress(MandemPlugin.mandemConfig.redisAddress).setClientName("Mandem") })

}