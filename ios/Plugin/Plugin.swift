import Capacitor
import Foundation
import PACECloudSDK

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(CloudSDK)
public class CloudSDK: CAPPlugin {
    @objc
    public func setup(_ call: CAPPluginCall) {
        guard let apiKey = call.getString(Constants.apiKey) else {
            call.reject("Failed setup due to a missing api key.")
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
        call.resolve()
    }
}
