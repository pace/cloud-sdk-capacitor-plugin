//
//  PluginAppData.swift
//  Plugin
//
//  Created by Patrick Niepel on 24.03.21.
//  Copyright © 2021 Max Lynch. All rights reserved.
//

import Foundation
import PACECloudSDK

struct PluginAppData: Codable {
    let title: String
    let subtitle: String
    let icons: [Icon]
    let appBaseUrl: String
    let appStartUrl: String
    let themeColor: String
    let backgroundColor: String
    let textColor: String

    init?(from appData: AppKit.AppData) {
        guard let manifest = appData.appManifest,
              let title = manifest.name,
              let subtitle = manifest.description,
              let icons = manifest.icons?.compactMap({ Icon(from: $0) }),
              let appBaseUrl = appData.appBaseUrl,
              let appStartUrl = appData.appStartUrl,
              let themeColor = manifest.themeColor,
              let backgroundColor = manifest.iconBackgroundColor,
              let textColor = manifest.textColor
        else { return nil }

        self.title = title
        self.subtitle = subtitle
        self.icons = icons
        self.appBaseUrl = appBaseUrl
        self.appStartUrl = appStartUrl
        self.themeColor = themeColor
        self.backgroundColor = backgroundColor
        self.textColor = textColor
    }

    struct Icon: Codable {
        let src: String
        let size: String
        let type: String

        init?(from appIcon: AppIcon) {
            guard let source = appIcon.source,
                  let size = appIcon.sizes,
                  let type = appIcon.type
            else { return nil }

            self.src = source
            self.size = size
            self.type = type
        }
    }
}
