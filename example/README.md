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
- Run `yarn install` from this directory *(`example` directory)*.
- Run `npx react-native run-android` from this directory *(`example` directory)*

## Try QR Code scanning

For your convenience, we've already uploaded an update of this demo application as a LiveBundle package. The main demo screen is now coming with the fancy LiveBundle logo. Just scan this QR Code from the demo application to see the cool new changes. Because this LiveBundle package contains both the dev and prod bundles, LiveBundle is going to ask you which bundle to install from the package. Just pick and choose any !

<h2 align="center">
	<img src="./assets/qrcode.png" alt="LiveBundle" width="200">
	<br>
</h2>

## Try Deep Link navigation

To try out Deep Link, the demo application can be in any state *(foreground/background or even not running at all)*.

If you are using a device and have Slack installed on the device, just send the following url to yourself on Slack.

`livebundle://packages?id=5a9fb29b-6f5b-43d0-8ea4-1220309efce4`

Then just tap on the link. It will trigger LiveBundle in the demo application, and download the package.

If you are using a simulator or a device without Slack, you can use `adb` to navigate to the deep link. Just run the following command from a terminal.

`$ adb shell am start -W -a android.intent.action.VIEW -d "livebundle://packages?id=5a9fb29b-6f5b-43d0-8ea4-1220309efce4" com.livebundle.example`

## Try uploading your own changes

**An Azure SAS token is needed to upload LiveBundle packages to the Azure Blob Storage. Ping @blemaire on Slack or via email blemaire@walmartlabs.com, to get a token**

Change anything in this demo application codebase, and upload your changes by running the following command. *Replace `<SAS_TOKEN>` with your token*.

Using npm

`$ LB_UPLOAD_AZURE_SASTOKEN=<SAS_TOKEN> npm run livebundle upload`

Using yarn

`$ LB_UPLOAD_AZURE_SASTOKEN=<SAS_TOKEN> yarn livebundle upload`



