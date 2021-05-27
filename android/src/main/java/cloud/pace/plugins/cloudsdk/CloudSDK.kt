package cloud.pace.plugins.cloudsdk

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import cloud.pace.plugins.cloudsdk.EnumUtils.searchEnum
import cloud.pace.sdk.PACECloudSDK
import cloud.pace.sdk.appkit.AppKit
import cloud.pace.sdk.appkit.communication.AppCallback
import cloud.pace.sdk.appkit.model.App
import cloud.pace.sdk.appkit.model.InvalidTokenReason
import cloud.pace.sdk.poikit.POIKit
import cloud.pace.sdk.poikit.poi.GasStation
import cloud.pace.sdk.poikit.poi.VisibleRegionNotificationToken
import cloud.pace.sdk.poikit.utils.LatLngBounds
import cloud.pace.sdk.poikit.utils.toVisibleRegion
import cloud.pace.sdk.utils.*
import com.getcapacitor.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import java.util.*

@NativePlugin
class CloudSDK : Plugin(), AppCallback {
    private var callbacks: MutableMap<String, Any> = mutableMapOf()
    private var poiObserver: VisibleRegionNotificationToken? = null

    @PluginMethod
    fun setup(call: PluginCall) {
        val apiKey = call.getString(API_KEY)

        if (apiKey == null) {
            call.reject("Failed setup: Missing Api Key")
            return
        }

        val callAuthenticationMode = call.getString(AUTHENTICATION_MODE) ?: WEB
        val callEnvironment = call.getString(ENVIRONMENT) ?: PRODUCTION

        val authenticationMode = try {
            AuthenticationMode.valueOf(callAuthenticationMode)
        } catch (e: Exception) {
            AuthenticationMode.WEB
        }

        val environment = searchEnum(Environment::class.java, callEnvironment)
                ?: Environment.PRODUCTION

        val configuration = Configuration(
                clientAppName = CLIENT_APP_NAME,
                clientAppVersion = CLIENT_APP_VERSION,
                clientAppBuild = CLIENT_APP_BUILD,
                apiKey = apiKey,
                authenticationMode = authenticationMode,
                environment = environment
        )

        PACECloudSDK.setup(context, configuration)
        call.resolve()
    }

    @PluginMethod
    fun isPoiInRange(call: PluginCall) {
        Handler(Looper.getMainLooper()).post {
            val poiId = call.getString(POI_ID)
            if (poiId == null) {
                call.reject("Failed isPoiInRange: Missing PoiID")
                return@post
            }

            AppKit.isPoiInRange(poiId) {
                val response = JSObject()
                response.put(RESULT, it)
                call.resolve(response)
            }
        }
    }

    @PluginMethod
    fun startApp(call: PluginCall) {
        val inputString = call.getString(URL)
        if (inputString == null) {
            call.reject("Failed startApp: Missing URL")
            return
        }

        AppKit.openAppActivity(context, inputString)
        call.resolve()
    }

    @PluginMethod
    fun startFuelingApp(call: PluginCall) {
        val inputString = call.getString(POI_ID)
        if (inputString == null) {
            call.reject("Failed startFuelingApp: Missing ID")
            return
        }

        AppKit.openFuelingApp(context, POI_ID)
        call.resolve()
    }

    @PluginMethod
    fun getNearbyGasStations(call: PluginCall) {
        val coordinate = call.getArray(COORDINATE)
        val radius = call.getDouble(RADIUS)

        if (coordinate == null || coordinate.length() != 2) {
            call.reject("Failed getNearbyGasStations: User location null or invalid")
            return
        }
        if (radius == null) {
            call.reject("Failed getNearbyGasStations: Missing value for radius")
            return
        }

        val visibleRegion = LatLngBounds(LatLng(coordinate.getDouble(1), coordinate.getDouble(0)), radius).toVisibleRegion()
        onMainThread {
            poiObserver?.invalidate()
            poiObserver = POIKit.observe(visibleRegion) {
                when (it) {
                    is Success -> {
                        val result = mutableListOf<PluginCofuStation>()
                        it.result.filterIsInstance(GasStation::class.java).forEach {
                            val address = Address(it.address?.countryCode, it.address?.city, it.address?.postalCode, it.address?.street, it.address?.houseNumber)
                            result.add(
                                    PluginCofuStation(
                                            it.id,
                                            it.name,
                                            address,
                                            listOf(it.longitude ?: 0.0, it.latitude ?: 0.0),
                                            it.isConnectedFuelingAvailable,
                                            it.updatedAt
                                    )
                            )
                        }

                        val response = JSObject()
                        response.put(RESULTS, JSArray(Gson().toJson(result)))
                        dispatchOnMainThread { call.resolve(response) }
                    }

                    is Failure -> {
                        dispatchOnMainThread { call.reject("Failed getNearbyGasStations: ${it.throwable.localizedMessage}") }
                    }
                }
            }
            poiObserver?.refresh()
        }
    }

    @PluginMethod
    fun respondToEvent(call: PluginCall) {
        val eventName = call.getString(NAME)
        if (eventName == null) {
            call.reject("Failed respondToEvent: Missing value for name")
            return
        }

        val event = searchEnum(PluginEvent::class.java, eventName)
        if (event == null) {
            call.reject("Failed respondToEvent due to an unregistered Event: $eventName")
        }

        when (event) {
            PluginEvent.TOKEN_INVALID -> {
                handleTokenInvalidEvent(call)
            }
        }
    }

    fun notify(event: PluginEvent, data: Map<String, Any> = hashMapOf()) {
        val convertedData = JSObject()
        convertedData.put(RESULT, data)
        notifyListeners(event.name, convertedData)
    }

    fun handleTokenInvalidEvent(call: PluginCall) {
        val token = call.getString(VALUE)
        if (token == null) {
            call.reject("Failed tokenInvalidEvent due to an invalid or missing token value")
            return
        }

        val id = call.getString(ID)
        if (id == null) {
            call.reject("Failed tokenInvalidEvent due missing value for ID")
        }

        val callback = callbacks[id] as? (String) -> Unit
        callback?.invoke(token)
        callbacks[id] = Unit
        call.resolve()
    }

    override fun getConfig(key: String, config: (String?) -> Unit) {
    }

    override fun logEvent(key: String, parameters: Map<String, Any>) {
    }


    override fun onClose() {
    }

    override fun onCustomSchemeError(context: Context?, scheme: String) {
    }

    override fun onDisable(host: String) {
    }

    override fun onImageDataReceived(bitmap: Bitmap) {
    }

    override fun onOpen(app: App?) {
    }

    override fun onOpenInNewTab(url: String) {
    }

    override fun onTokenInvalid(reason: InvalidTokenReason, oldToken: String?, onResult: (String) -> Unit) {
        val id = UUID.randomUUID().toString()
        callbacks[id] = onResult
        notify(PluginEvent.TOKEN_INVALID, mapOf(ID to id))
    }

    override fun setUserProperty(key: String, value: String, update: Boolean) {
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

        const val POI_ID = "poiId"
        const val URL = "url"
        const val RESULT = "result"
        const val RESULTS = "results"
        const val COORDINATE = "coordinate"
        const val RADIUS = "radius"

        const val ID = "id"
        const val NAME = "name"
        const val VALUE = "value"
    }
}
