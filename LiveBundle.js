import { NativeModules, Platform } from "react-native";
import { setCustomSourceTransformer } from "react-native/Libraries/Image/resolveAssetSource";

export class LiveBundle {
  /**
   * Gets prefixed azure url
   * @param {string} p The prefix to append to the url.
   * Can be an empty string to get raw base azure url.
   */
  getAzureUrl(p) {
    return `${this.azureUrl}${p}${this.sasToken}`;
  }

  /**
   * Initializes LiveBundle
   * This method should be called by client app on launch
   */
  initialize() {
    console.log("[LiveBundle] initialize()");
    this.sasToken = NativeModules.LiveBundle.AZURE_SASTOKEN;
    this.azureUrl = NativeModules.LiveBundle.AZURE_URL;
    this.state = {};
    this.getState().then((state) => {
      this.state = state;
      if (this.state.isBundleInstalled) {
        setCustomSourceTransformer((resolver) => {
          const res = resolver.scaledAssetPath();
          const { hash, name, type } = resolver.asset;
          res.uri = this.getAzureUrl(`assets/${hash}/${name}.${type}`);
          return res;
        });
      }
    });
  }

  /**
   * Returns current LiveBundle state
   */
  async getState() {
    console.log("[LiveBundle] getState()");
    return NativeModules.LiveBundle.getState();
  }

  /**
   * Retrieves metadata of a LiveBundle package
   * @param {string} packageId The id of the package
   */
  async getPackageMetadata(packageId) {
    console.log(`[LiveBundle] getPackageMetadata(${packageId})`);
    const res = await fetch(
      this.getAzureUrl(`packages/${packageId}/metadata.json`)
    );
    if (!res.ok) {
      throw new Error(
        `[LiveBundle] getPackageMetadata request failed : ${res.status} ${(
          await res.text()
        ).toString()}`
      );
    }
    return res.json();
  }

  /**
   * Retrieves metadata of a LiveBundle live session
   * @param {string} sessionId The id of the session
   */
  async getLiveSessionMetadata(sessionId) {
    console.log(`[LiveBundle] getLiveSessionMetadata(${sessionId})`);
    const res = await fetch(
      this.getAzureUrl(`sessions/${sessionId}/metadata.json`)
    );
    if (!res.ok) {
      throw new Error(
        `[LiveBundle] getLiveSessionMetadata request failed : ${res.status} ${(
          await res.text()
        ).toString()}`
      );
    }
    return res.json();
  }

  /**
   * Launches LiveBundle UI
   */
  launchUI() {
    console.log("[LiveBundle] launchUI()");
    NativeModules.LiveBundle.launchUI();
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
    const pkgMetadata = await this.getLiveSessionMetadata(sessionId);
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
        `[LiveBundle] donwloadBundleFlavor no dev bundle found in package ${packageId}`
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
