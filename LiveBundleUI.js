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

export class LiveBundleUI extends Component<{}> {
  constructor(props) {
    super(props);
    this.state = {
      bundleId: undefined,
      isDownloadCompleted: false,
      isScanCompleted: false,
      isScanInitiated: false,
      packageId: props?.packageId,
      packageMetadata: undefined,
      sessionId: props?.sessionId,
      error: undefined,
    };
  }

  componentWillUnmount() {
    console.log(`componentWillUnmount`);
  }

  render() {
    const {
      bundleId,
      isDownloadCompleted,
      isScanCompleted,
      isScanInitiated,
      packageId,
      packageMetadata,
      sessionId,
      error,
    } = this.state;
    let screen;
    if (error) {
      screen = (
        <View style={styles.subContainer}>
          <Image
            resizeMode="contain"
            style={styles.logo}
            source={require("./assets/logo.png")}
          />
          <Text style={styles.error}>{error.message}</Text>
        </View>
      );
    } else if (isDownloadCompleted) {
      // If we have a bundle ready to be installed, install
      // it and exist the menu
      livebundle.installBundle().catch((error) => {
        this.setState({ error });
      });
      BackHandler.exitApp();
    } else if (sessionId) {
      console.log(`sessionId: ${sessionId}`);
      // If we have a session id just launch the live session
      livebundle.launchLiveSession(sessionId).catch((error) => {
        this.setState({ error });
      });
      BackHandler.exitApp();
    } else if (packageId && !packageMetadata && !bundleId) {
      // If we have a packageId but no bundleId yet then we need
      // to retrieve the package metadata to look at what bundles
      // the package contains
      screen = (
        <View style={styles.subContainer}>
          <Image
            resizeMode="contain"
            style={styles.logo}
            source={require("./assets/logo.png")}
          />
          <Text style={styles.bottomText}>Loading Package</Text>
        </View>
      );
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
                this.setState({
                  bundleId: platformBundles.find((b) => b.dev).id,
                });
              }}
            >
              <Text style={styles.buttonText}>Dev</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.button}
              onPress={() => {
                this.setState({
                  bundleId: platformBundles.find((b) => !b.dev).id,
                });
              }}
            >
              <Text style={styles.buttonText}>Prod</Text>
            </TouchableOpacity>
          </View>
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
    } else if (isScanInitiated && !isScanCompleted) {
      // If the scanning has been triggered, show the scanner
      screen = (
        <RNCamera
          ref={(ref) => {
            this.camera = ref;
          }}
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
              this.setState({ sessionId: data.replace("s:", "") });
            } else {
              this.setState({ packageId: data });
            }
          }}
        />
      );
    } else {
      // LiveBundle menu screen
      const {
        isBundleInstalled,
        isSessionStarted,
        packageId,
        bundleId,
      } = livebundle.state;
      screen = (
        <View style={styles.subContainer}>
          <Image
            resizeMode="contain"
            style={styles.logo}
            source={require("./assets/logo.png")}
          />
          <TouchableOpacity
            style={styles.button}
            onPress={() => {
              this.setState({ isScanInitiated: true });
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
            <Text
              style={styles.bottomText2}
            >{`Connected to live session`}</Text>
          )}
        </View>
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
