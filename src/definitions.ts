declare module '@capacitor/core' {
  interface PluginRegistry {
    CloudSDK: CloudSDKPlugin;
  }
}

export enum AuthenticationMode {
  WEB = 'web',
  NATIVE = 'native',
}

export enum Environment {
  PRODUCTION = 'production',
  DEVELOPMENT = 'development',
  STAGE = 'stage',
  SANDBOX = 'sandbox',
}

export interface Configuration {
  /**
   * Use this property to provide the plugin with your PACE SDK API Key
   */
  apiKey: string;

  /**
   * If you use native logins, then this should be set to 'AuthenticationMode.NATIVE'.
   * Defaults to 'AuthenticationMode.WEB'
   */
  authenticationMode?: AuthenticationMode;

  /**
   * Set your environment to 'Environment.STAGE' or 'Environment.SANDBOX' while developing or running tests.
   * Defaults to 'Environment.PRODUCTION'
   */
  environment?: Environment;
}

export enum PaymentMethod {
  PAYPAL = 'paypal',
  APPLE_PAY = 'applePay',
}

export interface GasStation {
  id: string;
  coordinates: [number, number][];
  brand: string;
  paymentMethods: PaymentMethod[];
}

export interface AppDataIcon {
  src: string;
  size: string;
  type: string;
}
export interface AppData {
  title: string;
  subtitle: string;
  icon: AppDataIcon;
  appBaseUrl: string;
  appStartUrl: string;
  themeColor: string;
  backgroundColor: string;
  textColor: string;
}

export enum PresetUrl {
  PACE_ID = 'paceID',
  PAYMENT = 'payment',
  TRANSACTIONS = 'transactions',
}

export interface CloudSDKPlugin {
  /**
   * Setup the plugin
   * @param config is the configuration provided to this method
   */
  setup(config: Configuration): Promise<boolean>;

  /**
   * Lists all available POI stations that support Connected Fueling
   * @param countries takes a list of two-letter ISO 3166 country code; if none are provided, all stations will be returned
   */
  listAvailableCoFuStations(
    countries?: string[],
  ): Promise<{ results: GasStation[] }>;

  /**
   * Returns all Apps that can be started from the users location. Make sure your users allowed
   * your app to use their location before calling this method, as it will not ask for it.
   */
  checkForLocalApps(): Promise<{ results: AppData[] }>;

  /**
   * Check if there is a App for the given GasStation Id at the current location
   * @param poiId is the Id of a Connected Fueling station
   */
  isPoiInRange(poiId: string): Promise<{ result: boolean }>;

  /**
   * Start an App via a url
   * @param url is the appStartUrl from an App, or one of predefined presetUrls
   */
  startApp(url: string | PresetUrl): Promise<boolean>;
}
