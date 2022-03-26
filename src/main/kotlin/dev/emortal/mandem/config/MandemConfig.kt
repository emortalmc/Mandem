package dev.emortal.mandem.config

@kotlinx.serialization.Serializable
data class MandemConfig(
    val redisAddress: String = "redis://172.17.0.1:6379",
    val enabled: Boolean = false,
    val address: String = "172.17.0.1",
    val port: String = "3306",
    val tableName: String = "",
    val username: String = "",
    val password: String = ""
)