package cloud.pace.plugins.cloudsdk

import java.util.*

data class PluginGasStation(
        val id: String?,
        val name: String?,
        val address: Address?,
        val coordinates: List<Double>?,
        val isConnectedFuelingAvailable: Boolean?,
        val lastUpdated: Date?,
        val fuelPrice: List<FuelPrice>,
        val openingHour: List<OpeningHour>
)

data class Address(
        val countryCode: String?,
        val city: String?,
        val zipCode: String?,
        val streetName: String?,
        val houseNumber: String?
)

data class FuelPrice(
        val fuelType: String,
        val productName: String?,
        val price: Double?,
        val unit: String,
        val currency: String?,
        val priceFormatting: String?,
        val updated: Long?
)

data class OpeningHour(
        val day: String,
        val hours: List<List<String>>
)