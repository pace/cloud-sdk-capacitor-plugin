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

export enum OpeningHourDay {
  MONDAY = 'monday',
  TUESDAY = 'tuesday',
  WEDNESDAY = 'wednesday',
  THURSDAY = 'thursday',
  FRIDAY = 'friday',
  SATURDAY = 'saturday',
  SUNDAY = 'sunday',
}

export enum FuelCurrency {
  EUR = 'EUR',
}

export enum FuelType {
  RON98 = 'ron98',
  RON98E5 = 'ron98e5',
  RON95E10 = 'ron95e10',
  DIESEL = 'diesel',
  E85 = 'e85',
  RON91 = 'ron91',
  RON95E5 = 'ron95e5',
  RON100 = 'ron100',
  DIESELGTL = 'dieselGtl',
  DIESELB7 = 'dieselB7',
  DIESELPREMIUM = 'dieselPremium',
  LPG = 'lpg',
  CNG = 'cng',
  LNG = 'lng',
  H2 = 'h2',
  TRUCKDIESEL = 'truckDiesel',
  ADBLUE = 'adBlue',
  TRUCKADBLUE = 'truckAdBlue',
  TRUCKDIESELPREMIUM = 'truckDieselPremium',
  TRUCKLPG = 'truckLpg',
  HEATINGOIL = 'heatingOil',
}

export enum FuelPriceUnit {
  LITRE = 'L',
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

export interface OpeningHour {
  day: OpeningHourDay;
  hours: [number, number][];
}

export interface FuelPrice {
  fuelType: FuelType;
  productName: string;
  price: number;
  priceFormatting: string;
  unit: FuelPriceUnit;
  currency: FuelCurrency;
  updated: number;
}

export interface GasStation {
  id: string;
  name: string;
  address?: Address;

  /**
   * A set of coordinates, as [lng, lat]
   */
  coordinates: [number, number];

  /**
   * The opening hours for the Gas Station
   */
  openingHours: OpeningHour[];

  /**
   * The available fuel prices
   */
  fuelPrices: FuelPrice[];

  /**
   * Indicates if the gasStations supports Connected Fueling
   */
  isConnectedFuelingAvailable: boolean;
  lastUpdated: Date;
}

export interface EventResponse {
  /**
   * The `id` must be the same as from the received event
   */
  id: string;

  /**
   * The `name` must be the same as from the received event
   */
  name: string;

  /**
   * In case the `value` is an object, make sure to stringify it first
   */
  value: string;
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
   * Method that can to be called whenever the client wants
   * to communicate to the plugin in regards to a received event
   * @param response is the response to a given event
   */
   respondToEvent(response: EventResponse): Promise<boolean>;

  /**
   * Returns a list of gasStations based on a given coordinate
   * @param coordinate is the coordinate to search from, as [lng, lat]
   * @param radius is the radius to search in, in meters
   */
  getNearbyGasStations(options: {
    coordinate: [number, number];
    radius: number;
  }): Promise<{ results: GasStation[] }>;

  /**
   * Check if there is a App for the given GasStation Id at the current location
   * @param poiId is the Id of a GasStation
   * @param coordinate is the location to check if the gas station is in range of, as [lng, lat]
   */
  isPoiInRange(options: { 
    poiId: string;
    coordinate: [number, number];
  }): Promise<{ result: boolean }>;

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
