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
    private let pluginQueue: DispatchQueue = .init(label: "plugin-cloudsdk", qos: .utility)
    private var poiKitManager: POIKit.POIKitManager?
    private var downloadTask: CancellablePOIAPIRequest?

    // Event callbacks
    private var callbacks: [String: Any] = [:]

    @objc
    public func setup(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            guard let apiKey = call.getString(Constants.apiKey.rawValue) else {
                self?.reject(call, "Failed setup due to a missing value for '\(Constants.apiKey.rawValue)'.")
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
            self?.poiKitManager = POIKit.POIKitManager(environment: environment)

            AppKit.shared.delegate = self

            self?.resolve(call)
        }
    }

    @objc
    public func getNearbyGasStations(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            guard let radius = call.getDouble(Constants.radius.rawValue) else {
                self?.reject(call, "Failed getNearbyGasStations due to a missing value for '\(Constants.radius.rawValue)'.")
                return
            }

            guard let coordinate = call.getArray(Constants.coordinate.rawValue, Double.self),
                  let lon = coordinate[safe: 0],
                  let lat = coordinate[safe: 1] else {
                self?.reject(call, "Failed getNearbyGasStations due to a missing value for '\(Constants.coordinate.rawValue)'.")
                return
            }

            let location = CLLocationCoordinate2D(latitude: lat, longitude: lon)

            self?.downloadTask?.cancel()
            self?.downloadTask = self?.poiKitManager?.fetchPOIs(poisOfType: .gasStation,
                                                                boundingBox: POIKit.BoundingBox(center: location, radius: radius),
                                                                forceLoad: true) { [weak self] result in
                switch result {
                case .success(let stations):
                    let pluginStations: [PluginGasStation] = stations.compactMap { PluginGasStation(from: $0) }
                    let responseData = CAPPluginCall.pluginResultData(for: [Constants.results.rawValue: pluginStations])
                    self?.resolve(call, responseData)

                case .failure(let error):
                    self?.reject(call, "Failed getNearbyGasStations with error \(error)")
                }
            }
        }
    }

    @objc
    public func isPoiInRange(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            guard let poiId = call.getString(Constants.poiId.rawValue) else {
                self?.reject(call, "Failed isPoiInRange due to a missing value for '\(Constants.poiId.rawValue)'.")
                return
            }

            AppKit.shared.isPoiInRange(id: poiId) { result in
                self?.resolve(call, [Constants.result.rawValue: result])
            }
        }
    }

    @objc
    public func startApp(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            guard let inputString = call.getString(Constants.url.rawValue) else {
                self?.reject(call, "Failed startApp due to a missing value for '\(Constants.url.rawValue)'.")
                return
            }

            self?.dispatchToMainThread {
                let appVC: AppViewController

                if let presetUrl = PACECloudSDK.URL(rawValue: inputString) {
                    appVC = AppKit.shared.appViewController(presetUrl: presetUrl)
                } else if URL(string: inputString) != nil {
                    // Check if the string is a valid URL before passing it to AppKit
                    // because the eventually resulting error callbacks
                    // cannot be used in the plugin context
                    appVC = AppKit.shared.appViewController(appUrl: inputString)
                } else {
                    self?.reject(call, "Failed startApp due to an invalid value for '\(Constants.url.rawValue)'")
                    return
                }

                self?.presentViewController(appVC: appVC, for: call)
            }
        }
    }

    @objc
    public func startFuelingApp(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            let poiId = call.getString(Constants.poiId.rawValue)

            self?.dispatchToMainThread {
                let appVC = AppKit.shared.appViewController(presetUrl: .fueling(id: poiId))
                self?.presentViewController(appVC: appVC, for: call)
            }
        }
    }

    @objc
    public func respondToEvent(_ call: CAPPluginCall) {
        pluginQueue.async { [weak self] in
            guard let eventName = call.getString(Constants.name.rawValue) else {
                self?.reject(call, "Failed respondToEvent due to a missing value for '\(Constants.name.rawValue)'.")
                return
            }

            guard let event = PluginEvent(rawValue: eventName) else {
                self?.reject(call, "Failed respondToEvent due to an unregistered event  - \(eventName).")
                return
            }

            switch event {
            case .tokenInvalid:
                self?.handleTokenInvalidEvent(with: call)
            }
        }
    }
}

extension CloudSDK {
    private func resolve(_ call: CAPPluginCall, _ data: PluginResultData? = nil) {
        dispatchToMainThread {
            if let data = data {
                call.resolve(data)
            } else {
                call.resolve()
            }
        }
    }

    private func reject(_ call: CAPPluginCall, _ errorMessage: String) {
        dispatchToMainThread {
            call.reject(errorMessage)
        }
    }

    private func presentViewController(appVC: AppViewController, for call: CAPPluginCall) {
        dispatchToMainThread {
            self.bridge.viewController.present(appVC, animated: true) {
                call.resolve()
            }
        }
    }

    private func dispatchToMainThread(_ block: @escaping () -> Void) {
        DispatchQueue.main.async {
            block()
        }
    }
}

// MARK: - Event handling
extension CloudSDK {
    func notify(_ event: PluginEvent, data: [String: Any] = [:]) {
        dispatchToMainThread { [weak self] in
            self?.notifyListeners(event.rawValue, data: data)
        }
    }

    func handleTokenInvalidEvent(with call: CAPPluginCall) {
        guard let token = call.getString(Constants.value.rawValue) else {
            reject(call, "Failed tokenInvalidEvent due to an invalid or missing token value for '\(Constants.value.rawValue)'.")
            return
        }

        guard let id = call.getString(Constants.id.rawValue) else {
            reject(call, "Failed tokenInvalidEvent due to an invalid or missing value for '\(Constants.id.rawValue)'.")
            return
        }

        let callback = callbacks[id] as? (String) -> Void
        callback?(token)
        callbacks[id] = nil

        resolve(call)
    }
}

// MARK: - AppKitDelegate
extension CloudSDK: AppKitDelegate {
    public func tokenInvalid(reason: AppKit.InvalidTokenReason, oldToken: String?, completion: @escaping ((String) -> Void)) {
        pluginQueue.async { [weak self] in
            let id = UUID().uuidString
            self?.callbacks[id] = completion

            var data: [String: Any] = [Constants.id.rawValue: id,
                                       Constants.reason.rawValue: reason.rawValue]

            if let oldToken = oldToken {
                data[Constants.oldToken.rawValue] = oldToken
            }

            self?.notify(.tokenInvalid, data: data)
        }
    }

    public func didFail(with error: AppKit.AppError) {}
    public func didReceiveAppDrawers(_ appDrawers: [AppKit.AppDrawer], _ appDatas: [AppKit.AppData]) {}
}
