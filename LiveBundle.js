import { NativeModules, Platform, Linking } from "react-native";
import { setCustomSourceTransformer } from "react-native/Libraries/Image/resolveAssetSource";

export class LiveBundle {
  /**
   * Initializes LiveBundle
   * This method should be called by client app on launch
   */
  initialize() {
    console.log("[LiveBundle] initialize()");
    if (!this.isInitialized) {
      const nm = NativeModules.LiveBundle;
      this.STORAGE_URL = nm.STORAGE_URL;
      this.STORAGE_URL_SUFFIX = nm.STORAGE_URL_SUFFIX;
      this.PACKAGE_ID = nm.PACKAGE_ID;
      this.BUNDLE_ID = nm.BUNDLE_ID;
      this.IS_BUNDLE_INSTALLED = nm.IS_BUNDLE_INSTALLED;
      this.IS_SESSION_STARTED = nm.IS_SESSION_STARTED;
      if (this.IS_BUNDLE_INSTALLED) {
        setCustomSourceTransformer((resolver) => {
          const { hash } = resolver.asset;
          const res = resolver.scaledAssetPath();
          const filename = res.uri.replace(/^.*[\/]/, "");
          const platformFileName = filename.replace(
            /(^.*)(\..+)/,
            `$1.${Platform.OS}$2`
          );
          res.uri = this.getUrl(`assets/${hash}/${platformFileName}`);
          return res;
        });
      }
      this.isInitialized = true;
    }
    if (Platform.OS === 'ios') {
      Linking.getInitialURL().then(url => {
        url && this.launchUIFromDeepLink(url);
      })
    }
  }

  /**
   * Gets full url to resource
   * @param {string} resourcePath Path to resource
   */
  getUrl(resourcePath) {
    return `${this.STORAGE_URL}/${resourcePath}${this.STORAGE_URL_SUFFIX ?? ""}`;
  }

  /**
   * Gets the metadata associated to a LiveBundle session or package
   * @param {string} type Either SESSION or PACKAGE
   * @param {string} id Session or Package id
   */
  async getMetadata(type, id) {
    console.log(`[LiveBundle] getMetadata(${type}, ${id})`);
    const res = await fetch(
      this.getUrl(
        `${type === "PACKAGE" ? "packages" : "sessions"}/${id}/metadata.json`
      )
    );
    if (!res.ok) {
      throw new Error(
        `[LiveBundle] getMetadata request failed : ${res.status} ${(
          await res.text()
        ).toString()}`
      );
    }
    return res.json();
  }

  /**
   * Gets the metadata associated to a LiveBundle package
   * @param {string} packageId The id of the package
   */
  async getPackageMetadata(packageId) {
    return this.getMetadata("PACKAGE", packageId);
  }

  /**
   * Gets the metadata associated to a LiveBundle session
   * @param {string} sessionId The id of the session
   */
  async getLiveSessionMetadata(sessionId) {
    return this.getMetadata("SESSION", sessionId);
  }

  /**
   * Launches LiveBundle UI
   */
  launchUI(props) {
    console.log("[LiveBundle] launchUI()");
    NativeModules.LiveBundle.launchUI(props);
  }

  /**
   * Launches LiveBundle UI from Deep Link URL
   */
  launchUIFromDeepLink(deepLinkUrl) {
    console.log(`[LiveBundle] launchUIFromDeepLink(${deepLinkUrl})`);
    if (deepLinkUrl === 'livebundle://menu') {
      return this.launchUI();
    }
    const reurl = /livebundle:\/\/(sessions|packages)\?id=(.+)/;
    const capture = reurl.exec(deepLinkUrl);
    if (capture) {
      const [,host, id] = capture;
      switch (host) {
        case 'packages':
          this.launchUI({packageId: id});
          break;
        case 'sessions':
          this.launchUI({sessionId: id});
          break;
      }
    }
  }

  /**
   * Resets to original app state
   * This will reload original application bundle
   */
  async reset() {
    console.log("[LiveBundle] reset()");
    setCustomSourceTransformer(undefined);
    return NativeModules.LiveBundle.reset();
  }

  /**
   * Downloads a LiveBundle bundle given its id
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   * @param {string} bundleId The id of the bundle to download
   */
  async downloadBundle(packageId, bundleId) {
    console.log(`[LiveBundle] downloadBundle(${packageId}, ${bundleId})`);
    return NativeModules.LiveBundle.downloadBundle(packageId, bundleId);
  }

  /**
   * Launches a LiveBundle live session (connecting to remote package) given the session id
   * @param {string} sessionId The id of the session to launch
   */
  async launchLiveSession(sessionId) {
    console.log(`[LiveBundle] launchLiveSession(${sessionId})`);
    const pkgMetadata = await this.getSessionMetadata(sessionId);
    return NativeModules.LiveBundle.launchLiveSession(pkgMetadata.host);
  }

  /**
   * Downloads a specific LiveBundle bundle flavor
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   * @param {string} flavor Either "dev" or "prod"
   */
  async donwloadBundleFlavor(packageId, flavor) {
    console.log(`[LiveBundle] donwloadBundleFlavor(${packageId}, ${flavor})`);
    const pkgMetadata = await this.getPackageMetadata(packageId);
    const bundle = pkgMetadata.bundles.find(
      (b) => (flavor === "dev" ? b.dev : !b.dev) && b.platform === Platform.OS
    );
    if (!bundle) {
      throw new Error(
        `[LiveBundle] donwloadBundleFlavor no ${flavor} bundle found in package ${packageId}`
      );
    }
    return this.downloadBundle(packageId, bundle.id);
  }

  /**
   * Downloads a development LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadDevBundle(packageId) {
    console.log(`[LiveBundle] donwloadDevBundle(${packageId})`);
    return this.donwloadBundleFlavor(packageId, "dev");
  }

  /**
   * Downloads a production LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadProdBundle(packageId) {
    console.log(`[LiveBundle] donwloadProdBundle(${packageId})`);
    return this.donwloadBundleFlavor(packageId, "prod");
  }

  /**
   * Installs a LiveBundle bundle after download
   * This will recreate the react application context with
   * the new bundle
   */
  async installBundle() {
    console.log("[LiveBundle] installBundle()");
    return NativeModules.LiveBundle.installBundle();
  }
}

const livebundle = new LiveBundle();
export default livebundle;

if (Platform.OS === 'ios') {
  Linking.addEventListener('url', ({url}) => {
    livebundle.launchUIFromDeepLink(url)
  })
}
