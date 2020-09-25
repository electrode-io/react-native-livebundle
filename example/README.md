<h2 align="center">
    <br>
	<img src="./assets/logo.png" alt="LiveBundle" width="200">
	<br>
    <br>
</h2>

# LiveBundle demo application *(Android Only)*

## Install and launch the application

- Start an Android simulator or connect an Android device.
- **Trash the top level `node_modules` directory it it exist** *(in parent directory, the one at the root of react-native-livebundle directory)*. We are looking at a way to avoid this step, but it is necessary as of now, otherwise the application will not work properly.
- From the `example` directory *(this directory)* :
  - Run `yarn install` from this directory *(the `example` directory)*.
 - Run `npx react-native run-android` from this directory *(the `example` directory)*

## Try QR Code scanning

For your convenience, we've already uploaded an update of this demo application as a LiveBundle package. Just scan this QR Code from the demo application to see the changes.

<h2 align="center">
	<img src="./assets/qrcode.png" alt="LiveBundle" width="200">
	<br>
</h2>

## Try Deep Link navigation

If you are using a device and have Slack installed on the device, just send the following url to yourself on Slack.

`livebundle://packages?id=97557a75-2236-4f8c-843c-927719e16c3b`

Then just tap on the link. It will trigger LiveBundle in the demo application, and download the package.

If you are using a simulator or a device without Slack, you can use `adb` to navigate to the deep link. Just run the following command from a terminal.

`$ adb shell am start -W -a android.intent.action.VIEW -d "livebundle://packages?id=97557a75-2236-4f8c-843c-927719e16c3b" com.livebundle.example`

## Try uploading your own changes

**An Azure SAS token is needed to upload LiveBundle packages to the Azure Blob Storage. Ping @blemaire on Slack or via email blemaire@walmartlabs.com, to get a token**

Change anything in this demo application codebase, and upload your changes by running the following command. *Replace `<SAS_TOKEN>` with your token*.

Using npm

`$ LB_STORAGE_AZURE_SASTOKEN==<SAS_TOKEN> npm run livebundle upload`

Using yarn

`$ LB_STORAGE_AZURE_SASTOKEN==<SAS_TOKEN> yarn livebundle upload`

## Development/Debbuging

The demo application can be used for development on the native module.\

#### Native Module Native changes *(in android directory)*

After any changes to native module native code, just run `npx react-native run-android`. React Native will rebuild the application, including the native module, and will install / relaunch the application.

#### Native Module JS changes *(in top level directory)*

If a metro bundler is running locally, any JS changes will be automatically picked up *(or just hit reload)*.
In case you are not using a local metro bundler *(to test built in -exo- bundle)*, you can just run `npx react-native run-android --no-packager` after making JS changes.

#### Debbuging Native Module Native code

You can either open and run in debug mode this example application in Android Studio or attach Android studio debugger to running application. Because the native module is linked with the application, its source code will be directly editable / debuggable from the project.

#### Debbuging Native Module JS code

Just open React Native developer menu in the application and tap on `Debug`.
