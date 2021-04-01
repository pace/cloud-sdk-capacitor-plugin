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

export interface Address {
  countryCode?: string;
  city?: string;
  zipCode?: string;
  suburb?: string;
  state?: string;
  street?: string;
  houseNumber?: string;
}

export interface GasStation {
  id: string;
  name: string;
  address?: Address;
  coordinates: [number, number][];
  isConnectedFuelingAvailable: boolean;
  lastUpdated: Date;
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
   * Returns a list of gasStations based on a given coordinate
   * @param coordinate is the coordinate to search from
   * @param radius is the radius to search in
   */
  getNearbyGasStations(options: {
    coordinate: [number, number];
    radius: number;
  }): Promise<{ results: GasStation[] }>;

  /**
   * Check if there is a App for the given GasStation Id at the current location
   * @param poiId is the Id of a GasStation
   */
  isPoiInRange(options: { poiId: string }): Promise<{ result: boolean }>;

  /**
   * Start an App via a url
   * @param url is the appStartUrl from an App, or one of predefined presetUrls
   */
  startApp(options: { url: string | PresetUrl }): Promise<boolean>;

  /**
   * Start an App for a given poiId
   * @param url is the Id of a GasStation
   */
  startFuelingApp(options: { poiId: string }): Promise<boolean>;
}
