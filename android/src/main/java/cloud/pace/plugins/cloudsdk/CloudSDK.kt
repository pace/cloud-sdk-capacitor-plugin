package cloud.pace.plugins.cloudsdk

import cloud.pace.plugins.cloudsdk.EnumUtils.searchEnum
import cloud.pace.sdk.PACECloudSDK
import cloud.pace.sdk.appkit.AppKit
import cloud.pace.sdk.poikit.POIKit
import cloud.pace.sdk.poikit.poi.GasStation
import cloud.pace.sdk.poikit.utils.LatLngBounds
import cloud.pace.sdk.poikit.utils.toVisibleRegion
import cloud.pace.sdk.utils.*
import com.getcapacitor.*
import com.google.android.gms.maps.model.LatLng

@NativePlugin
class CloudSDK : Plugin() {
    private var cofuStationFetchRunning: Boolean = false

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
        val poiId = call.getString(POI_ID)
        if (poiId == null) {
            call.reject("Failed isPoiInRange: Missing PoiID")
            return
        }

        AppKit.isPoiInRange(poiId) {
            val response = JSObject()
            response.put(RESULT, it)
            call.resolve(response)
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
        val userLocation = call.getArray(USER_LOCATION)
        val radius = call.getDouble(RADIUS)

        if (userLocation == null || userLocation.length() != 2) {
            call.reject("Failed getNearbyGasStations: User location null or invalid")
            return
        }
        if (radius == null) {
            call.reject("Failed getNearbyGasStations: Missing value for radius")
            return
        }

        val visibleRegion = LatLngBounds(LatLng(userLocation.getDouble(1), userLocation.getDouble(0)), radius).toVisibleRegion()
        POIKit.observe(visibleRegion) {
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
                    response.put(RESULTS, result)
                    dispatchOnMainThread { call.resolve(response) }
                }

                is Failure -> {
                    dispatchOnMainThread { call.reject("Failed getNearbyGasStations: ${it.throwable.localizedMessage}") }
                }
            }
        }
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
        const val USER_LOCATION = "user_location"
        const val RADIUS = "radius"
    }
}