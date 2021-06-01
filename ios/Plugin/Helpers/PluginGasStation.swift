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
    let fuelPrices: [FuelPrice]
    let coordinates: [Double]
    let isConnectedFuelingAvailable: Bool
    let lastUpdated: Date

    private(set) var openingHours: [OpeningHour] = []

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
        self.fuelPrices = poiStation.prices.compactMap { FuelPrice(from: $0, format: attributes.priceFormat) }
        self.openingHours = parseOpeningHours(from: attributes.openingHours)
    }

    private func parseOpeningHours(from poiOpeningHours: PCPOICommonOpeningHours?) -> [OpeningHour] {
        guard let poiOpeningHours = poiOpeningHours,
              let rules = poiOpeningHours.rules
        else { return [] }

        let openingHours: [OpeningHour] = PCPOICommonOpeningHours.Rules.PCPOIDays.allCases
            .compactMap { day in
                let hours: [[String]] = rules
                    .filter { ($0.days?.contains(day) ?? false) && $0.action == .open }
                    .compactMap { $0.timespans }
                    .flatMap { $0 }
                    .compactMap {
                        guard let from = $0.from,
                              let to = $0.to
                        else { return nil }
                        return [from, to]
                    }

                return OpeningHour(day: day.rawValue, hours: hours)
            }
        return openingHours
    }
}

extension PluginGasStation {
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

extension PluginGasStation {
    struct OpeningHour: Codable {
        let day: String
        let hours: [[String]]

        init?(day: String, hours: [[String]]) {
            guard !hours.isEmpty else { return nil }
            self.day = day
            self.hours = hours
        }
    }
}

extension PluginGasStation {
    struct FuelPrice: Codable {
        let fuelType: String
        let productName: String
        let price: Double
        let unit: String
        let currency: String
        let priceFormatting: String
        let updated: Double

        init?(from poiFuelPrice: PCPOIFuelPrice, format: String?) {
            guard let attributes = poiFuelPrice.attributes,
                  let fuelType = attributes.fuelType,
                  let productName = attributes.productName,
                  let price = attributes.price,
                  let currency = attributes.currency,
                  let format = format,
                  let updated = attributes.updatedAt
            else { return nil }

            self.fuelType = fuelType.rawValue
            self.productName = productName
            self.price = price
            self.unit = "L"
            self.currency = currency.rawValue
            self.priceFormatting = format
            self.updated = updated.timeIntervalSince1970
        }
    }
}
