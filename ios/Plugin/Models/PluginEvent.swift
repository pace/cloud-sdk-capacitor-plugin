//
//  PluginEvent.swift
//  Plugin
//
//  Created by Patrick Niepel on 31.03.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation

typealias EventResponse = Codable

enum PluginEvent: String {
    case tokenInvalid
}

struct GetAccessTokenEventResponse: EventResponse {
    let id: String
    let reason: String
    var oldToken: String?
}
