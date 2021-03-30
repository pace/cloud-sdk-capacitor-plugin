package cloud.pace.plugins.cloudsdk

import java.util.*

data class PluginCofuStation(
        val id: String?,
        val name: String?,
        val address: Address?,
        val coordinates: List<Double>?,
        val isConnectedFuelingAvailable: Boolean?,
        val lastUpdated: Date?
)

data class Address(
        val countryCode: String?,
        val city: String?,
        val zipCode: String?,
        val streetName: String?,
        val houseNumber: String?
)