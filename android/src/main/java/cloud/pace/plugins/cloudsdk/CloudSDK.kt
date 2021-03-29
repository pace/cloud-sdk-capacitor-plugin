package cloud.pace.plugins.cloudsdk

import cloud.pace.plugins.cloudsdk.EnumUtils.searchEnum
import cloud.pace.sdk.PACECloudSDK
import cloud.pace.sdk.api.API
import cloud.pace.sdk.api.geojson.GeoJSONAPI.geoJSON
import cloud.pace.sdk.api.geojson.generated.request.geoJSON.GetBetaGeojsonPoisAPI
import cloud.pace.sdk.api.geojson.generated.request.geoJSON.GetBetaGeojsonPoisAPI.getBetaGeojsonPois
import cloud.pace.sdk.appkit.AppKit
import cloud.pace.sdk.utils.*
import com.getcapacitor.*

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
    fun listAvailableCoFuStations(call: PluginCall) {
        if (cofuStationFetchRunning) {
            call.reject("Failed listAvailableCoFuStations: Already running")
            return
        }

        val countries = call.getArray(COUNTRIES)
        val countryString = countries.join(",")
        cofuStationFetchRunning = true
        API.geoJSON.getBetaGeojsonPois(
                fieldsgasStation = "stationName,brand",
                filterpoiType = GetBetaGeojsonPoisAPI.FilterpoiType.GASSTATION,
                filteronlinePaymentMethod = "paydirekt,paypal,creditcard,sepa,pacePay",
                filtercountry = countryString,
                filterconnectedFueling = "true"
        ).enqueue {
            onResponse = {
                val body = it.body()
                if (it.isSuccessful && body != null) {
                    val result = mutableListOf<PluginCofuStation>()
                    body.features?.forEach {
                        val onlinePaymentMethods = it.properties?.get("onlinePaymentMethods")
                        val paymentMethodList = if (onlinePaymentMethods is List<*>) onlinePaymentMethods.filterIsInstance(String::class.java) else listOf()
                        result.add(PluginCofuStation(
                                it.id,
                                it.geometry?.coordinates?.map { it.toDouble() },
                                it.properties?.get("brand").toString(),
                                paymentMethodList)
                        )
                    }

                    val response = JSObject()
                    response.put(RESULTS, result)
                    dispatchOnMainThread { call.resolve(response) }
                } else
                    dispatchOnMainThread { call.reject("Failed listAvailableCoFuStations: Response not successful or body null") }
                cofuStationFetchRunning = false
            }

            onFailure = {
                dispatchOnMainThread { call.reject("Failed listAvailableCoFuStations: Error: ${it?.localizedMessage}") }
                cofuStationFetchRunning = false
            }
        }
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
    fun checkForLocalApps(call: PluginCall) {
        AppKit.requestLocalApps {
            val response = JSObject()
            response.put(RESULTS, (it as? Success)?.result)
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

    companion object {
        const val CLIENT_APP_NAME = "clientAppName"
        const val CLIENT_APP_VERSION = "clientAppVersion"
        const val CLIENT_APP_BUILD = "clientAppBuild"
        const val API_KEY = "apiKey"
        const val AUTHENTICATION_MODE = "authenticationMode"
        const val WEB = "Web"
        const val ENVIRONMENT = "environment"
        const val PRODUCTION = "production"

        const val COUNTRIES = "countries"
        const val POI_ID = "poiId"
        const val URL = "url"
        const val RESULT = "result"
        const val RESULTS = "results"

    }
}