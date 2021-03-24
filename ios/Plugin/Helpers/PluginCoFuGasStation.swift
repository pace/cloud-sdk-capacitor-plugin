//
//  PluginCoFuGasStation.swift
//  Plugin
//
//  Created by Patrick Niepel on 25.03.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation
import PACECloudSDK

struct PluginCoFuGasStation: Codable {
    let id: String
    let coordinates: [Double]
    let brand: String
    let paymentMethods: [String]

    init?(from feature: PCGeoJSONGeoJsonFeature) {
        guard let id = feature.id,
              let geometry = feature.geometry,
              let coordinates = geometry.coordinates?.map({ Double($0) }),
              let properties = feature.properties,
              let brand = properties["brand"] as? String
        else { return nil }

        self.id = id
        self.coordinates = coordinates
        self.brand = brand
        self.paymentMethods = properties["onlinePaymentMethods"] as? [String] ?? []
    }
}
