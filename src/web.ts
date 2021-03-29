import { WebPlugin } from '@capacitor/core';
import { CloudSDKPlugin, Configuration, GasStation } from './definitions';

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

  async getNearbyGasStations(
    _radius: number = 20,
  ): Promise<{ results: GasStation[] }> {
    throw new Error('This method not is available in web');
  }

  async getGasStations(
    _poiIds: string[] = [],
  ): Promise<{ results: GasStation[] }> {
    throw new Error('This method not is available in web');
  }

  async isPoiInRange(_poiId: string): Promise<{ result: boolean }> {
    throw new Error('This method not is available in web');
  }

  async startApp(_appStartUrl: string): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async startFuelingApp(_poiId: string): Promise<boolean> {
    throw new Error('This method not is available in web');
  }
}

const CloudSDK = new CloudSDKWeb();

export { CloudSDK };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CloudSDK);
