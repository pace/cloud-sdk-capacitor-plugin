package cloud.pace.plugins.cloudsdk

import cloud.pace.sdk.appkit.communication.InvalidTokenReason

enum class PluginEvent(name: String) {
    TOKEN_INVALID("tokenInvalid")
}

interface EventNotificationData {
    val id: String
}

data class GetAccessTokenNotification(
    override val id: String,
    val reason: InvalidTokenReason,
    var oldToken: String? = null
): EventNotificationData