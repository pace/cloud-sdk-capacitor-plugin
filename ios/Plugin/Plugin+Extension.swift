//
//  Plugin+Extension.swift
//  Plugin
//
//  Created by Patrick Niepel on 14.04.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Capacitor
import Foundation

extension CAPPluginCall {
    static func pluginResultData<T: Encodable>(for encodable: T) -> PluginResultData? {
        guard let data = try? JSONEncoder().encode(encodable),
              let jsonObject = try? JSONSerialization.jsonObject(with: data) as? PluginResultData else {
            return nil
        }

        return jsonObject
    }
}
