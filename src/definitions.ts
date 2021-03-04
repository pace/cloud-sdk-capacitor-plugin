declare module '@capacitor/core' {
  interface PluginRegistry {
    CloudSDK: CloudSDKPlugin;
  }
}

export interface CloudSDKPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
