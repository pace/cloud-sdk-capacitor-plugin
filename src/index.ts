import { registerPlugin } from '@capacitor/core';

import type { CloudSDKPlugin } from './definitions';

const CloudSDK = registerPlugin<CloudSDKPlugin>('CloudSDK', {
  web: () => import('./web').then(m => new m.CloudSDKWeb()),
});

export * from './definitions';
export { CloudSDK };
