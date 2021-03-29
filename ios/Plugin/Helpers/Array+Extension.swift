//
//  Array+Extension.swift
//  Plugin
//
//  Created by Patrick Niepel on 29.03.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation

extension Array {
    subscript(safe index: Int) -> Element? {
        indices ~= index ? self[index] : nil
    }
}
