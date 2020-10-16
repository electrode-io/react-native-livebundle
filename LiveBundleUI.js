import React, { Component } from "react";
import {
  AppRegistry,
  Image,
  StyleSheet,
  Text,
  View,
  BackHandler,
  TouchableOpacity,
  Platform,
} from "react-native";
import { RNCamera } from "react-native-camera";
import livebundle from "./LiveBundle";

const ErrorScreen = ({ errorMessage }) => (
  <View style={styles.subContainer}>
    <Image
      resizeMode="contain"
      style={styles.logo}
      source={require("./assets/logo.png")}
    />
    <Text style={styles.error}>{errorMessage}</Text>
  </View>
);

const LoadingScreen = () => (
  <View style={styles.subContainer}>
    <Image
      resizeMode="contain"
      style={styles.logo}
      source={require("./assets/logo.png")}
    />
    <Text style={styles.bottomText}>Loading Package</Text>
  </View>
);

const BundleFlavorSelectionScreen = ({ platformBundles, onBundleSelected }) => (
  <View style={styles.subContainer}>
    <Image
      resizeMode="contain"
      style={styles.logo}
      source={require("./assets/logo.png")}
    />
    <Text style={styles.text}>Select bundle flavor to install</Text>
    <TouchableOpacity
      style={styles.button}
      onPress={() => {
        onBundleSelected(platformBundles.find((b) => b.dev).id);
      }}
    >
      <Text style={styles.buttonText}>Dev</Text>
    </TouchableOpacity>
    <TouchableOpacity
      style={styles.button}
      onPress={() => {
        onBundleSelected(platformBundles.find((b) => !b.dev).id);
      }}
    >
      <Text style={styles.buttonText}>Prod</Text>
    </TouchableOpacity>
  </View>
);

const QRCodeScannerScreen = ({ onQrCodeRead }) => (
  <RNCamera
    captureAudio={false}
    style={styles.preview}
    type={RNCamera.Constants.Type.back}
    flashMode={RNCamera.Constants.FlashMode.off}
    androidCameraPermissionOptions={{
      title: "Permission to use camera",
      message: "We need your permission to use your camera",
      buttonPositive: "Ok",
      buttonNegative: "Cancel",
    }}
    onBarCodeRead={({ data }) => {
      if (data.startsWith("s:")) {
        onQrCodeRead({ sessionId: data.replace("s:", "") });
      } else {
        onQrCodeRead({ packageId: data });
      }
    }}
  />
);

const MainScreen = ({
  bundleId,
  packageId,
  isBundleInstalled,
  isSessionStarted,
  onScanInitiated,
}) => (
  <View style={styles.subContainer}>
    <Image
      resizeMode="contain"
      style={styles.logo}
      source={require("./assets/logo.png")}
    />
    <TouchableOpacity
      style={styles.button}
      onPress={() => {
        onScanInitiated();
      }}
    >
      <Text style={styles.buttonText}>Scan</Text>
    </TouchableOpacity>
    {(isBundleInstalled || isSessionStarted) && (
      <TouchableOpacity
        style={styles.button}
        onPress={() => {
          livebundle.reset();
          BackHandler.exitApp();
        }}
      >
        <Text style={styles.buttonText}>Reset</Text>
      </TouchableOpacity>
    )}
    {isBundleInstalled && (
      <Text style={styles.bottomText}>{`packageId: ${packageId}`}</Text>
    )}
    {isBundleInstalled && (
      <Text style={styles.bottomText2}>{`bundleId: ${bundleId}`}</Text>
    )}
    {isSessionStarted && (
      <Text style={styles.bottomText2}>{`Connected to live session`}</Text>
    )}
  </View>
);

export class LiveBundleUI extends Component<{}> {
  constructor(props) {
    super(props);
    this.state = {
      bundleId: undefined,
      isDownloadCompleted: false,
      isScanInitiated: false,
      packageId: props?.packageId,
      packageMetadata: undefined,
      sessionId: props?.sessionId,
      error: undefined,
    };
  }

  render() {
    const {
      bundleId,
      isDownloadCompleted,
      isScanInitiated,
      packageId,
      packageMetadata,
      sessionId,
      error,
    } = this.state;
    let screen;
    if (error) {
      screen = <ErrorScreen errorMessage={error.message} />;
    } else if (isDownloadCompleted) {
      // If we have a bundle ready to be installed, install
      // it and exist the menu
      livebundle.installBundle().catch((error) => {
        this.setState({ error });
      });
      BackHandler.exitApp();
    } else if (sessionId) {
      // If we have a session id just launch the live session
      livebundle.launchLiveSession(sessionId).catch((error) => {
        this.setState({ error });
      });
      BackHandler.exitApp();
    } else if (packageId && !packageMetadata && !bundleId) {
      // If we have a packageId but no bundleId yet then we need
      // to retrieve the package metadata to look at what bundles
      // the package contains
      screen = <LoadingScreen />;
      livebundle
        .getPackageMetadata(packageId)
        .then((packageMetadata) => {
          this.setState({ packageMetadata });
        })
        .catch((error) => this.setState({ error }));
    } else if (packageId && packageMetadata && !bundleId) {
      // If we have the package metadata, we need to check if there
      // is one or more bundle(s) in the package
      const platformBundles = packageMetadata.bundles.filter(
        (b) => b.platform === Platform.OS
      );
      if (platformBundles.length === 0) {
        this.setState({
          error: new Error(
            `No bundle for ${Platform.OS} platform in package ${packageId}`
          ),
        });
      } else if (platformBundles.length === 1) {
        // Only one bundle in package for this platform.
        // Immediately proceed to download.
        this.setState({ bundleId: platformBundles[0].id });
      } else {
        // More than one bundle in package for this platform.
        // Ask user to choose bundle flavor -for now-.
        screen = (
          <BundleFlavorSelectionScreen
            platformBundles={platformBundles}
            onBundleSelected={(bundleId) => this.setState({ bundleId })}
          />
        );
      }
    } else if (packageId && bundleId) {
      // If we have a packageId and a bundleId then we can download
      // the bundle from the storage
      livebundle
        .downloadBundle(packageId, bundleId)
        .then(() => {
          this.setState({
            isDownloadCompleted: true,
          });
        })
        .catch((error) => this.setState({ error }));
    } else if (isScanInitiated) {
      // If the scanning has been triggered, show the scanner
      screen = (
        <QRCodeScannerScreen
          onQrCodeRead={({ packageId, sessionId }) =>
            this.setState({ isScanInitiated: false, packageId, sessionId })
          }
        />
      );
    } else {
      // LiveBundle main screen
      const {
        IS_BUNDLE_INSTALLED,
        IS_SESSION_STARTED,
        PACKAGE_ID,
        BUNDLE_ID,
      } = livebundle;
      screen = (
        <MainScreen
          bundleId={BUNDLE_ID}
          packageId={PACKAGE_ID}
          isBundleInstalled={IS_BUNDLE_INSTALLED}
          isSessionStarted={IS_SESSION_STARTED}
          onScanInitiated={() => this.setState({ isScanInitiated: true })}
        />
      );
    }

    return <View style={styles.container}>{screen}</View>;
  }
}

const styles = StyleSheet.create({
  button: {
    backgroundColor: "rgb(65,136,214)",
    borderColor: "rgb(25,96,174)",
    borderRadius: 3,
    borderWidth: 0.3,
    alignItems: "center",
    margin: 5,
    width: 165,
    padding: 6,
  },
  buttonText: {
    color: "#FFFFFF",
    fontSize: 18,
  },
  container: {
    flex: 1,
    flexDirection: "column",
    backgroundColor: "white",
  },
  logo: {
    height: 120,
    marginBottom: 50,
  },
  preview: {
    flex: 1,
    justifyContent: "flex-end",
    alignItems: "center",
  },
  subContainer: {
    justifyContent: "center",
    flex: 1,
    alignItems: "center",
  },
  text: {
    fontSize: 18,
    marginBottom: 20,
  },
  bottomText: {
    fontSize: 14,
    marginTop: 20,
  },
  bottomText2: {
    fontSize: 14,
    marginTop: 1,
  },
  error: {
    fontSize: 15,
    textAlign: "center",
    margin: 10,
  },
});

AppRegistry.registerComponent("LiveBundleUI", () => LiveBundleUI);
