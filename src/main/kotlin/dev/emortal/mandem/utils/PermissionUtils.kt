package dev.emortal.mandem.utils

import com.velocitypowered.api.proxy.Player
import dev.emortal.mandem.MandemPlugin.Companion.luckperms
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.luckperms.api.model.user.User

object PermissionUtils {
    val mini = MiniMessage.miniMessage()
    val userToPlayerMap = mutableMapOf<User, Player>()
    val playerAdapter = luckperms.getPlayerAdapter(Player::class.java)

    val Player.lpUser: User get() {
        val user = playerAdapter.getUser(this)
        userToPlayerMap.putIfAbsent(user, this)
        return user
    }

    val Player.prefix: String? get() = lpUser.cachedData.metaData.prefix
    val Player.suffix: String? get() = lpUser.cachedData.metaData.suffix

    val Player.displayName: Component get() = mini.deserialize("$prefix $username")
}