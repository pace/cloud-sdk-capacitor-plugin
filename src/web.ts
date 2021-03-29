import { WebPlugin } from '@capacitor/core';
import {
  AppData,
  CloudSDKPlugin,
  Configuration,
  GasStation,
} from './definitions';

export class CloudSDKWeb extends WebPlugin implements CloudSDKPlugin {
  constructor() {
    super({
      name: 'CloudSDK',
      platforms: ['web'],
    });
  }

  async setup(config: Configuration): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async listAvailableCoFuStations(
    countries = [],
  ): Promise<{ results: GasStation[] }> {
    throw new Error('This method not is available in web');
  }

  async checkForLocalApps(): Promise<{ results: AppData[] }> {
    throw new Error('This method not is available in web');
  }

  async isPoiInRange(poiId: string): Promise<{ result: boolean }> {
    throw new Error('This method not is available in web');
  }

  async startApp(appStartUrl: string): Promise<boolean> {
    throw new Error('This method not is available in web');
  }

  async startFuelingApp(poiId: string): Promise<boolean> {
    throw new Error('This method not is available in web');
  }
}

const CloudSDK = new CloudSDKWeb();

export { CloudSDK };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CloudSDK);
