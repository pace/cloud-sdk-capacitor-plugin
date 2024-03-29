package cloud.pace.plugins.cloudsdk

import android.location.Location
import android.webkit.URLUtil
import cloud.pace.plugins.cloudsdk.EnumUtils.searchEnum
import cloud.pace.sdk.PACECloudSDK
import cloud.pace.sdk.appkit.AppKit
import cloud.pace.sdk.appkit.communication.*
import cloud.pace.sdk.poikit.POIKit
import cloud.pace.sdk.poikit.poi.*
import cloud.pace.sdk.poikit.utils.LatLngBounds
import cloud.pace.sdk.poikit.utils.toVisibleRegion
import cloud.pace.sdk.utils.*
import com.getcapacitor.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import timber.log.Timber
import java.util.*

@NativePlugin
class CloudSDK : Plugin() {
    private var callbacks: MutableMap<String, Any> = mutableMapOf()
    private var poiObserver: VisibleRegionNotificationToken? = null
    private val defaultCallback = object : AppCallbackImpl() {
        override fun getAccessToken(
            reason: InvalidTokenReason,
            oldToken: String?,
            onResult: (GetAccessTokenResponse) -> Unit
        ) {
            val id = UUID.randomUUID().toString()
            callbacks[id] = onResult

            val data = GetAccessTokenNotification(id, reason)
            oldToken?.let {
                data.oldToken = it
            }

            notify(PluginEvent.TOKEN_INVALID, data)
        }
    }

    @PluginMethod
    fun setup(call: PluginCall) {
        val apiKey = call.getString(API_KEY)

        if (apiKey == null) {
            call.reject("Failed setup: Missing Api Key")
            return
        }

        val callAuthenticationMode = call.getString(AUTHENTICATION_MODE) ?: WEB
        val callEnvironment = call.getString(ENVIRONMENT) ?: PRODUCTION

        val authenticationMode = if (callAuthenticationMode.equals("native", true)) AuthenticationMode.NATIVE else AuthenticationMode.WEB

        val environment = searchEnum(Environment::class.java, callEnvironment) ?: Environment.PRODUCTION

        val configuration = Configuration(
            clientAppName = CLIENT_APP_NAME,
            clientAppVersion = CLIENT_APP_VERSION,
            clientAppBuild = CLIENT_APP_BUILD,
            apiKey = apiKey,
            authenticationMode = authenticationMode,
            environment = environment,
            oidConfiguration = null
        )

        PACECloudSDK.setup(context, configuration)
        call.resolve()
    }

    @PluginMethod
    fun isPoiInRange(call: PluginCall) {
        onMainThread {
            val coordinate = call.getArray(COORDINATE)
            if (coordinate == null || coordinate.length() != 2) {
                call.reject("Failed isPoiInRange: User location null or invalid")
                return@onMainThread
            }
            val poiId = call.getString(POI_ID)
            if (poiId == null) {
                call.reject("Failed isPoiInRange: Missing PoiID")
                return@onMainThread
            }
            val location = Location("location")
            location.latitude = coordinate.getDouble(1)
            location.longitude = coordinate.getDouble(0)

            val response = JSObject()
            response.put(RESULT, POIKit.isPoiInRange(poiId, location))
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

        val presetUrl = when (inputString) {
            DASHBOARD -> cloud.pace.sdk.utils.URL.dashboard
            FUELING -> cloud.pace.sdk.utils.URL.fueling
            PACE_ID -> cloud.pace.sdk.utils.URL.paceID
            PAYMENT -> cloud.pace.sdk.utils.URL.payment
            TRANSACTIONS -> cloud.pace.sdk.utils.URL.transactions
            else -> null
        }

        when {
            presetUrl != null -> {
                AppKit.openAppActivity(context, presetUrl, callback = defaultCallback)
            }
            URLUtil.isValidUrl(inputString) -> {
                AppKit.openAppActivity(context, inputString, callback = defaultCallback)
            }
            else -> {
                call.reject("Failed startApp: Invalid value for URL")
            }
        }

        call.resolve()
    }

    @PluginMethod
    fun startFuelingApp(call: PluginCall) {
        val poiId = call.getString(POI_ID)
        if (poiId == null) {
            call.reject("Failed startFuelingApp: Missing POI ID")
            return
        }

        AppKit.openFuelingApp(context, poiId, callback = defaultCallback)
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

        val visibleRegion = LatLngBounds(
            LatLng(coordinate.getDouble(1), coordinate.getDouble(0)),
            radius
        ).toVisibleRegion()
        onMainThread {
            poiObserver?.invalidate()
            poiObserver = POIKit.observe(visibleRegion) {
                when (it) {
                    is Success -> {
                        val result = mutableListOf<PluginGasStation>()
                        it.result.filterIsInstance(GasStation::class.java).forEach {
                            val address = Address(
                                it.address?.countryCode,
                                it.address?.city,
                                it.address?.postalCode,
                                it.address?.street,
                                it.address?.houseNumber
                            )

                            result.add(
                                PluginGasStation(
                                    it.id,
                                    it.name,
                                    address,
                                    listOf(it.longitude ?: 0.0, it.latitude ?: 0.0),
                                    it.isConnectedFuelingAvailable,
                                    it.updatedAt,
                                    it.prices.map { price ->
                                        FuelPrice(
                                            price.type.value,
                                            price.name,
                                            price.price,
                                            "L",
                                            it.currency,
                                            it.priceFormat,
                                            it.updatedAt?.time?.div(1000)
                                        )
                                    },
                                    parseOpeningHours(it.openingHours)
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

    private fun parseOpeningHours(poiOpeningHours: List<OpeningHours>?): List<OpeningHour> {
        val result = mutableListOf<OpeningHour>()

        enumValues<Day>().forEach { day ->
            val daysOpeningHours = mutableListOf<List<String>>()
            poiOpeningHours?.forEach {
                if (it.rule == OpeningRule.OPEN && it.days.contains(day)) {
                    it.hours.firstOrNull()?.let { hour ->
                        daysOpeningHours.add(listOf(hour.from, hour.to))
                    }
                }
            }

            if (daysOpeningHours.isNotEmpty()) {
                result.add(OpeningHour(day.name, daysOpeningHours))
            }
        }

        return result
    }

    @PluginMethod
    fun respondToEvent(call: PluginCall) {
        Timber.i("Received event response")

        val id = call.getString(ID)
        if (id == null) {
            Timber.e("Failed respondToEvent: Missing value for ID")
            call.reject("Failed respondToEvent: Missing value for ID")
            return
        }

        val callback = callbacks.remove(id)

        if (callbacks == null) {
            Timber.e("Failed respondToEvent: No callback for given ID")
            call.reject("Failed respondToEvent: No callback for given ID")
            return
        }

        val eventName = call.getString(NAME)
        if (eventName == null) {
            Timber.e("Failed respondToEvent: Missing value for name")
            call.reject("Failed respondToEvent: Missing value for name")
            return
        }

        val event = searchEnum(PluginEvent::class.java, eventName)
        if (event == null) {
            Timber.e("Failed respondToEvent due to an unregistered Event: $eventName")
            call.reject("Failed respondToEvent due to an unregistered Event: $eventName")
        }

        Timber.i("Event data")

        when (event) {
            PluginEvent.TOKEN_INVALID -> {
                handleTokenInvalidEvent(call, callback)
            }
        }
    }

    fun notify(event: PluginEvent, data: EventNotificationData) {
        val convertedData = JSObject(Gson().toJson(data))
        notifyListeners(event.name, convertedData)
        Timber.i("Sent SDK notification ${event.name} with id: ${data.id}")
    }

    fun handleTokenInvalidEvent(call: PluginCall, callback: Any?) {
        val callback = callback as? (GetAccessTokenResponse) -> Unit
        val token = call.getString(VALUE)
        if (token == null) {
            call.reject("Failed tokenInvalidEvent due to an invalid or missing token value")
            return
        }

        val tokenResponse = GetAccessTokenResponse(token)

        callback?.invoke(tokenResponse)
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

        const val POI_ID = "poiId"
        const val URL = "url"
        const val RESULT = "result"
        const val RESULTS = "results"
        const val COORDINATE = "coordinate"
        const val RADIUS = "radius"

        const val ID = "id"
        const val NAME = "name"
        const val VALUE = "value"

        const val DASHBOARD = "dashboard"
        const val FUELING = "fueling"
        const val PACE_ID = "paceID"
        const val PAYMENT = "payment"
        const val TRANSACTIONS = "transactions"

        const val REASON = "reason"
        const val OLD_TOKEN = "oldToken"
    }
}
