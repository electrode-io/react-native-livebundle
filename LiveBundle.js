import { NativeModules, Platform } from "react-native";
import {setCustomSourceTransformer} from 'react-native/Libraries/Image/resolveAssetSource';

export class LiveBundle {
  constructor() {
    console.log(`[LiveBundle] ctor`);
    this.sasToken = NativeModules.LiveBundle.AZURE_SASTOKEN;
    this.azureUrl = NativeModules.LiveBundle.AZURE_URL;

    NativeModules.LiveBundle.getState().then(res => {
      console.log(`[LiveBundle] ctor. getState() call response: ${JSON.stringify(res, null, 2)}`);
      this.res = res;
      if (res.isBundleInstalled) {
        setCustomSourceTransformer((resolver) => {
          const res = resolver.scaledAssetPath();
          const {hash, name, type} = resolver.asset;
          res.uri = this.getAzureUrl(`assets/${hash}/${name}.${type}`);
          console.log(`Asset uri is : ${res.uri}`);
          return res;
        });
      }
    });
  }

  /**
   * Gets prefixed azure url
   * @param {string} p The prefix to append to the url.
   * Can be an empty string to get raw base azure url.
   */
  getAzureUrl(p) {
    return `${this.azureUrl}${p}${this.sasToken}`;
  }

  /**
   * Retrieves metadata of a LiveBundle package
   * @param {string} packageId The id of the package
   */
  async getPackageMetadata(packageId) {
    console.log(`[LiveBundle] getPackageMetadata(${packageId})`);
    let res;
    try {
      res = await fetch(this.getAzureUrl(`packages/${packageId}/metadata.json`));
    } catch(e) {
      console.log(`Oops: ${e}`);
    }
    console.log(`[LiveBundle] getPackageMetadata response: ${JSON.stringify(res, null, 2)}`);
    return res.json();
  }

   /**
   * Retrieves metadata of a LiveBundle live session
   * @param {string} sessionId The id of the session
   */
  async getLiveSessionMetadata(sessionId) {
    console.log(`[LiveBundle] getLiveSessionMetadata(${sessionId})`);
    const res = await fetch(this.getAzureUrl(`sessions/${sessionId}/metadata.json`));
    console.log(`[LiveBundle] getLiveSessionMetadata response: ${JSON.stringify(res, null, 2)}`);
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

  async launchLiveSession(sessionId) {
    console.log(`[LiveBundle] launchLiveSession(${sessionId})`);
    const pkgMetadata = await this.getLiveSessionMetadata(sessionId);
    console.log(`metadata :${pkgMetadata}`)
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
    const bundle = pkgMetadata.bundles.find(b => (flavor === "dev" ? b.dev : !b.dev) && b.platform === Platform.OS);
    if (!bundle) {
      throw new Error(`[LiveBundle] donwloadBundleFlavor no dev bundle found in package ${packageId}`);
    }
    return this.downloadBundle(packageId, bundle.id);
  }

  /**
   * Downloads a development LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadDevBundle(packageId) {
    console.log(`[LiveBundle] donwloadDevBundle(${packageId})`);
    return this.donwloadBundleFlavor(packageId, "dev")
  }

  /**
   * Downloads a production LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadProdBundle(packageId) {
    console.log(`[LiveBundle] donwloadProdBundle(${packageId})`);
    return this.donwloadBundleFlavor(packageId, "prod")
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
