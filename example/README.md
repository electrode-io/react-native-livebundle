<h2 align="center">
    <br>
	<img src="./assets/logo.png" alt="LiveBundle" width="200">
	<br>
    <br>
</h2>

# LiveBundle demo application _(Android Only)_

## Trying it out

**Unless mentionned otherwise, all following commands should be run fom the this directory _(the `example` directory)_**

### Initial Setup

- Clone this repository on your workstation
- **From the `example` directory**, run `yarn install`<br/>**_!! Do not run this command from the top level directory!!_**

### Upload an application change

- Change something in [App.js](./App.js). We recommend updating some text so that the change is clearly visible
- Run `yarn livebundle upload`

Per [`livebundle.yml`](./livebundle.yml) configuration file, LiveBundle will generate android and iOS dev bundles and will then upload them to the storage _(in this case, a local directory)_, then it will display a QR Code and Deep Link to install the changes in the application. Keep the QR Code and Deep Link handy, we will need them soon.

### Start the HTTP server

Because we'll need to download the bundles from the application _(either from an emulator or device)_ we can just start an HTTP server to serve any files kept in LiveBundle storage directory. A simple HTTP server can be easily started as follow

- From the `~/.livebundle/storage` directory run `npx http-server`

### Install and launch the application

- Start an Android simulator or connect an Android device.
- Run `adb reverse tcp:8080 tcp:8080`
- **Trash the top level `node_modules` directory it it exist** _(in parent directory, the one at the root of react-native-livebundle directory)_. We are looking at a way to avoid this step, but it is necessary as of now, otherwise the application will not work properly.
- Run `yarn install`
- Run `npx react-native run-android --no-packager`

### Try QR Code scanning

From the application, launch the LiveBundle menu by either tapping `LiveBundle` from the React Native developer menu, or by simply tapping the `Launch LiveBundle Menu` from the application _(which launches the LiveBundle menu programmatically)_.

Then tap the `Scan` button. If this is the first time launch, the application will ask for permission to access the camera.

Scan the QR Code that was generated earlier. LiveBundle will download and install the bundle. You should see the changes you made earlier.

### Try Deep Link navigation

If you have Slack installed on the device, just send yourself the Deep Link url that was logged to the terminal from your earlier run of `livebundle upload` command.

Then just tap on the link. It will trigger LiveBundle in the demo application, and download the package.

Alternatively you can use `adb` to navigate to the Deep Link. Just run the following command from a terminal.

```bash
$ adb shell am start -W -a android.intent.action.VIEW -d "[DEEP_LINK_URL]" io.livebundle.example
```

Just replace `[DEEP_LINK_URL]` with your Deep Link url.

### Trying Reset

## Development/Debbuging

The demo application can be used for development on the native module.\

### Native Module Native changes _(in android directory)_

After any changes to native module native code, just run `npx react-native run-android`. React Native will rebuild the application, including the native module, and will install / relaunch the application.

### Native Module JS changes _(in top level directory)_

If a metro bundler is running locally, any JS changes will be automatically picked up _(or just hit reload)_.
In case you are not using a local metro bundler _(to test built in -exo- bundle)_, you can just run `npx react-native run-android --no-packager` after making JS changes.

### Debbuging Native Module Native code

You can either open and run in debug mode this example application in Android Studio or attach Android studio debugger to running application. Because the native module is linked with the application, its source code will be directly editable / debuggable from the project.

### Debbuging Native Module JS code

Just open React Native developer menu in the application and tap on `Debug`.
