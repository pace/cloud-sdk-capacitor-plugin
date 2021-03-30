import Capacitor
import CoreLocation
import Foundation
import PACECloudSDK

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CloudSDK)
public class CloudSDK: CAPPlugin {
    private var poiKitManager: POIKit.POIKitManager?
    private var downloadTask: CancellablePOIAPIRequest?

    @objc
    public func setup(_ call: CAPPluginCall) {
        guard let apiKey = call.getString(Constants.apiKey.rawValue) else {
            call.reject("Failed setup due to a missing value for '\(Constants.apiKey.rawValue)'.")
            return
        }

        let callAuthenticationMode = call.getString(Constants.authenticationMode.rawValue) ?? Constants.web.rawValue
        let callEnvironment = call.getString(Constants.environment.rawValue) ?? Constants.production.rawValue

        let authenticationMode = PACECloudSDK.AuthenticationMode(rawValue: callAuthenticationMode) ?? .web
        let environment = PACECloudSDK.Environment(rawValue: callEnvironment) ?? .production

        let configuration: PACECloudSDK.Configuration = .init(apiKey: apiKey,
                                                              authenticationMode: authenticationMode,
                                                              environment: environment)

        PACECloudSDK.shared.setup(with: configuration)
        poiKitManager = POIKit.POIKitManager(environment: environment)

        call.resolve()
    }

    @objc
    public func getNearbyGasStations(_ call: CAPPluginCall) {
        guard let radius = call.getDouble(Constants.radius.rawValue) else {
            call.reject("Failed getNearbyGasStations due to a missing value for '\(Constants.radius.rawValue)'.")
            return
        }

        guard let coordinate = call.getArray(Constants.coordinate.rawValue, Double.self),
              let lon = coordinate[safe: 0],
              let lat = coordinate[safe: 1] else {
            call.reject("Failed getNearbyGasStations due to a missing value for '\(Constants.coordinate.rawValue)'.")
            return
        }

        let location = CLLocationCoordinate2D(latitude: lat, longitude: lon)

        downloadTask?.cancel()
        downloadTask = poiKitManager?.fetchPOIs(poisOfType: .gasStation, boundingBox: POIKit.BoundingBox(center: location, radius: radius), forceLoad: true) { [weak self] result in
            switch result {
            case .success(let stations):
                let pluginStations: [PluginGasStation] = stations.compactMap { PluginGasStation(from: $0) }
                self?.dispatchToMainThread(call.resolve([Constants.results.rawValue: pluginStations]))

            case .failure(let error):
                self?.dispatchToMainThread(call.reject("Failed listAvailableCoFuStations with error \(error.localizedDescription)"))
            }
        }
    }

    @objc
    public func isPoiInRange(_ call: CAPPluginCall) {
        guard let poiId = call.getString(Constants.poiId.rawValue) else {
            call.reject("Failed isPoiInRange due to a missing value for '\(Constants.poiId.rawValue)'.")
            return
        }

        AppKit.shared.isPoiInRange(id: poiId) { result in
            call.resolve([Constants.result.rawValue: result])
        }
    }

    @objc
    public func startApp(_ call: CAPPluginCall) {
        guard let inputString = call.getString(Constants.url.rawValue) else {
            call.reject("Failed startApp due to a missing value for '\(Constants.url.rawValue)'.")
            return
        }

        let appVC: AppViewController

        if let presetUrl = PACECloudSDK.URL(rawValue: inputString) {
            appVC = AppKit.shared.appViewController(presetUrl: presetUrl)
        } else if URL(string: inputString) != nil {
            // Check if the string is a valid URL before passing it to AppKit
            // because the eventually resulting error callbacks
            // cannot be used in the plugin context
            appVC = AppKit.shared.appViewController(appUrl: inputString)
        } else {
            call.reject("Failed startApp due to an invalid value for '\(Constants.url.rawValue)'")
            return
        }

        presentViewController(appVC: appVC, for: call)
    }

    @objc
    public func startFuelingApp(_ call: CAPPluginCall) {
        let poiId = call.getString(Constants.poiId.rawValue)
        let appVC = AppKit.shared.appViewController(presetUrl: .fueling(id: poiId))
        presentViewController(appVC: appVC, for: call)
    }

    private func presentViewController(appVC: AppViewController, for call: CAPPluginCall) {
        dispatchToMainThread(
            self.bridge.viewController.present(appVC, animated: true) {
                call.resolve()
            }
        )
    }

    private func dispatchToMainThread(_ block: @autoclosure @escaping () -> Void) {
        DispatchQueue.main.async {
            block()
        }
    }
}
