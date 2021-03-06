#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(CloudSDK, "CloudSDK",
           CAP_PLUGIN_METHOD(setup, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getNearbyGasStations, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(isPoiInRange, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startApp, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(startFuelingApp, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(respondToEvent, CAPPluginReturnPromise);
)
