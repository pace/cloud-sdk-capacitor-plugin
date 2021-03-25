package cloud.pace.plugins.cloudsdk

import cloud.pace.sdk.PACECloudSDK
import cloud.pace.sdk.utils.AuthenticationMode
import cloud.pace.sdk.utils.Configuration
import cloud.pace.sdk.utils.Environment
import com.getcapacitor.*

@NativePlugin
class CloudSDK : Plugin() {
    @PluginMethod
    fun setup(call: PluginCall) {
        val apiKey = call.getString(API_KEY)

        if (apiKey == null) {
            call.reject("Missing Api Key")
            return
        }

        val callAuthenticationMode = call.getString(AUTHENTICATION_MODE) ?: WEB
        val callEnvironment = call.getString(ENVIRONMENT) ?: PRODUCTION

        val authenticationMode = AuthenticationMode.valueOf(callAuthenticationMode)
        val environment = Environment.valueOf(callEnvironment)

        val configuration: Configuration = Configuration(
                clientAppName = CLIENT_APP_NAME,
                clientAppVersion = CLIENT_APP_VERSION,
                clientAppBuild = CLIENT_APP_BUILD,
                apiKey = apiKey,
                authenticationMode = authenticationMode
        )

        PACECloudSDK.setup(context, configuration)
        call.resolve()
    }

    companion object {
        const val CLIENT_APP_NAME = "clientAppName"
        const val CLIENT_APP_VERSION = "clientAppVersion"
        const val CLIENT_APP_BUILD = "clientAppBuild"
        const val API_KEY = "apiKey"
        const val AUTHENTICATION_MODE = "authenticationMode"
        const val WEB = "Web"
        const val ENVIRONMENT = "environment"
        const val PRODUCTION = "production"
    }
}