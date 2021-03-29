import Capacitor
import Foundation
import PACECloudSDK

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CloudSDK)
public class CloudSDK: CAPPlugin {
    private var checkForLocalAppsCallback: CAPPluginCall?
    private var isCofuStationsFetchRunning = false

    @objc
    public func setup(_ call: CAPPluginCall) {
        guard let apiKey = call.getString(Constants.apiKey) else {
            call.reject("Failed setup due to a missing value for '\(Constants.apiKey)'.")
            return
        }

        let callAuthenticationMode = call.getString(Constants.authenticationMode) ?? Constants.web
        let callEnvironment = call.getString(Constants.environment) ?? Constants.production

        let authenticationMode = PACECloudSDK.AuthenticationMode(rawValue: callAuthenticationMode) ?? .web
        let environment = PACECloudSDK.Environment(rawValue: callEnvironment) ?? .production

        let configuration: PACECloudSDK.Configuration = .init(apiKey: apiKey,
                                                              authenticationMode: authenticationMode,
                                                              environment: environment)

        PACECloudSDK.shared.setup(with: configuration)
        AppKit.shared.delegate = self

        call.resolve()
    }

    @objc
    public func listAvailableCoFuStations(_ call: CAPPluginCall) {
        guard !isCofuStationsFetchRunning else {
            call.reject("Failed listAvailableCoFuStations due to: Fetch already running...")
            return
        }

        let countries = call.getArray(Constants.countries, String.self)
        let countryString = countries?.filter { !$0.isEmpty }.joined(separator: ",")
        let request = GeoJSONAPI.GetBetaGeojsonPois.Request(fieldsgasStation: "stationName,brand",
                                                            filterpoiType: .gasStation,
                                                            filteronlinePaymentMethod: "paydirekt,paypal,creditcard,sepa,pacePay,applepay",
                                                            filtercountry: countryString,
                                                            filterconnectedFueling: "true")
        isCofuStationsFetchRunning = true

        API.GeoJSON.client.makeRequest(request) { [weak self] response in
            defer {
                self?.isCofuStationsFetchRunning = false
            }

            switch response.result {
            case .success(let result):
                let cofuStations: [PluginCoFuGasStation] = result.success?.features?.compactMap { PluginCoFuGasStation(from: $0) } ?? []
                self?.dispatchToMainThread(call.resolve([Constants.results: cofuStations]))

            case .failure(let error):
                self?.dispatchToMainThread(call.reject("Failed listAvailableCoFuStations with error \(error.localizedDescription)"))
            }
        }
    }

    @objc
    public func isPoiInRange(_ call: CAPPluginCall) {
        guard let poiId = call.getString(Constants.poiId) else {
            call.reject("Failed isPoiInRange due to a missing value for '\(Constants.poiId)'.")
            return
        }

        AppKit.shared.isPoiInRange(id: poiId) { result in
            call.resolve([Constants.result: result])
        }
    }

    @objc
    public func checkForLocalApps(_ call: CAPPluginCall) {
        checkForLocalAppsCallback = call
        AppKit.shared.requestLocalApps()
    }

    @objc
    public func startApp(_ call: CAPPluginCall) {
        guard let inputString = call.getString(Constants.url) else {
            call.reject("Failed startApp due to a missing value for '\(Constants.url)'.")
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
            call.reject("Failed startApp due to an invalid value for '\(Constants.url)'")
            return
        }

        presentViewController(appVC: appVC, for: call)
    }

    @objc
    public func startFuelingApp(_ call: CAPPluginCall) {
        let poiId = call.getString(Constants.poiId)
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

extension CloudSDK: AppKitDelegate {
    public func didFail(with error: AppKit.AppError) {}

    public func didReceiveAppDrawers(_ appDrawers: [AppKit.AppDrawer], _ appDatas: [AppKit.AppData]) {
        let appDataList: [PluginAppData] = appDatas.compactMap { PluginAppData(from: $0) }

        DispatchQueue.main.async { [weak self] in
            self?.checkForLocalAppsCallback?.resolve([Constants.results: appDataList])
            self?.checkForLocalAppsCallback = nil
        }
    }
}
