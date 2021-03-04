import { WebPlugin } from '@capacitor/core';
import { CloudSDKPlugin } from './definitions';

export class CloudSDKWeb extends WebPlugin implements CloudSDKPlugin {
  constructor() {
    super({
      name: 'CloudSDK',
      platforms: ['web'],
    });
  }

  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}

const CloudSDK = new CloudSDKWeb();

export { CloudSDK };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CloudSDK);
