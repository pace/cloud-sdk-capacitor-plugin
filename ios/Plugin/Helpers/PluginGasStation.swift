//
//  PluginGasStation.swift
//  Plugin
//
//  Created by Patrick Niepel on 25.03.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation
import PACECloudSDK

struct PluginGasStation: Codable {
    let id: String
    let name: String
    let address: Address?
    let coordinates: [Double]
    let isConnectedFuelingAvailable: Bool
    let lastUpdated: Date

    init?(from poiStation: POIKit.GasStation) {
        guard let id = poiStation.id,
              let coordinate = poiStation.coordinate,
              let attributes = poiStation.attributes,
              let name = attributes.stationName,
              let lastUpdated = poiStation.lastUpdated
        else { return nil }

        self.id = id
        self.name = name
        self.coordinates = [coordinate.longitude, coordinate.latitude]
        self.isConnectedFuelingAvailable = poiStation.isConnectedFuelingAvailable
        self.lastUpdated = lastUpdated
        self.address = Address(from: attributes.address)
    }

    struct Address: Codable {
        let countryCode: String?
        let city: String?
        let zipCode: String?
        let street: String?
        let houseNumber: String?

        init(from poiAddress: PCPOIGasStation.Attributes.Address?) {
            self.countryCode = poiAddress?.countryCode
            self.city = poiAddress?.city
            self.zipCode = poiAddress?.postalCode
            self.street = poiAddress?.street
            self.houseNumber = poiAddress?.houseNo
        }
    }
}
