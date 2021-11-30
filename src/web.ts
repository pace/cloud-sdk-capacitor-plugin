import { WebPlugin } from '@capacitor/core';
import {
  CloudSDKPlugin,
  Configuration,
  GasStation,
  PresetUrl,
} from './definitions';

export class CloudSDKWeb extends WebPlugin implements CloudSDKPlugin {
  constructor() {
    super({
      name: 'CloudSDK',
      platforms: ['web'],
    });
  }

  async setup(_config: Configuration): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async respondToEvent(_response: EventResponse): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async getNearbyGasStations(_options: {
    coordinate: [number, number];
    radius: number;
  }): Promise<{ results: GasStation[] }> {
    throw new Error('This method not is available in web');
  }

  async isPoiInRange(_options: {
    poiId: string;
  }): Promise<{ result: boolean }> {
    throw new Error('This method not is available in web');
  }

  async startApp(_options: { url: string | PresetUrl }): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async startFuelingApp(_options: { poiId: string }): Promise<boolean> {
    throw new Error('This method not is available in web');
  }
}

const CloudSDK = new CloudSDKWeb();

export { CloudSDK };

import { registerWebPlugin } from '@capacitor/core';
import { EventResponse } from '.';
registerWebPlugin(CloudSDK);
