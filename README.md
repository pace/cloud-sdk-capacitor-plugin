# Cloud SDK Capacitor Plugin

[![License](https://img.shields.io/badge/license-MIT-green.svg?style=flat)](LICENSE.md)

## Initial Setup

Run the following commands in the root directory of this repository:

```bash
npm install
npm run build
```

## Android
### Updating the PACECloudSDK
### Making Code Changes

## iOS
### Updating the PACECloudSDK

1. Head over to `/ios`
2. Open the `Podfile`
3. Adjust the line `pod 'PACECloudSDK', :git => 'https://github.com/pace/cloud-sdk-ios', :tag => '9.1.0'` to use the required version
4. Run `pod update` to fetch the new version

### Making Code Changes

1. Head over to `/ios`
2. Open the `Plugin.xcworkspace` file to start the project in XCode
3. The `Plugin` folder contains all the neccessary `.swift` files.
<img width="203" alt="Screenshot 2021-11-24 at 14 36 22" src="https://user-images.githubusercontent.com/22814132/143248493-01090132-03aa-4888-a2e6-b1ba4239353e.png">


## Publishing a new version

All you have to do is running the following command in the root directory:

```bash
npm version $THE_NEW_VERSION

# Example: npm version 0.0.14
```

After running the command a commit will automatically be created. Pushing the commit and the correspoding new tag will result in the new version being published.

## PACECloudSDK Documentation

- [Android](https://pace.github.io/cloud-sdk-android/)
- [iOS](https://pace.github.io/cloud-sdk-ios)

## License

This project is licensed under the terms of the MIT license. See the [LICENSE](LICENSE.md) file.