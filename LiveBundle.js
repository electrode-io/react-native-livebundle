import { NativeModules, Platform } from "react-native";
import {setCustomSourceTransformer} from 'react-native/Libraries/Image/resolveAssetSource';

export class LiveBundle {
  initialize({
    /** Azure storage url including container name **/
    azureUrl
  }) {
    NativeModules.LiveBundle.initialize(azureUrl);
    this.azureUrl = azureUrl.endsWith('/') ? azureUrl : `${azureUrl}/`;
  }

  /**
   * Gets prefixed azure url
   * @param {string} p The prefix to append to the url.
   * Can be an empty string to get raw base azure url.
   */
  getAzureUrl(p) {
    return `${this.azureUrl}${p}`;
  }

  /**
   * Retrieves metadata of a LiveBundle package
   * @param {string} packageId The id of the package
   */
  async getPackageMetadata(packageId) {
    const res = await fetch(this.getAzureUrl(`packages/${packageId}/metadata.json`));
    return res.json();
  }

   /**
   * Retrieves metadata of a LiveBundle live session
   * @param {string} sessionId The id of the session
   */
  async getLiveSessionMetadata(sessionId) {
    const res = await fetch(this.getAzureUrl(`sessions/${sessionId}/metadata.json`));
    return res.json();
  }

  /**
   * Launches LiveBundle UI
   */
  launchUI() {
    NativeModules.LiveBundle.launchUI();
  }

  /**
   * Resets to original app state
   * This will reload original application bundle
   */
  async reset() {
    setCustomSourceTransformer(undefined);
    return NativeModules.LiveBundle.reset();
  }

  /**
   * Downloads a LiveBundle bundle given its id
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   * @param {string} bundleId The id of the bundle to download
   */
  async downloadBundle(packageId, bundleId) {
    return NativeModules.LiveBundle.downloadBundle(packageId, bundleId);
  }

  async launchLiveSession(sessionId) {
    console.log(`here cocote`)
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
    const pkgMetadata = await this.getPackageMetadata(packageId);
    const bundle = pkgMetadata.bundles.find(b => (flavor === "dev" ? b.dev : !b.dev) && b.platform === Platform.OS);
    if (!bundle) {
      throw new Error(`No dev bundle found in package ${packageId}`);
    }
    return this.downloadBundle(packageId, bundle.id);
  }

  /**
   * Downloads a development LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadDevBundle(packageId) {
    return this.donwloadBundleFlavor(packageId, "dev")
  }

  /**
   * Downloads a production LiveBundle bundle
   * @param {string} packageId The id of the LiveBundle package containing the bundle
   */
  async donwloadProdBundle(packageId) {
    return this.donwloadBundleFlavor(packageId, "prod")
  }

  /**
   * Installs a LiveBundle bundle after download
   * This will recreate the react application context with
   * the new bundle
   */
  async installBundle() {
    setCustomSourceTransformer((resolver) => {
      const res = resolver.scaledAssetPath();
      const {hash, name, type} = resolver.asset;
      res.uri = this.getAzureUrl(`assets/${hash}/${name}.${type}`);
      return res;
    });
    return NativeModules.LiveBundle.installBundle();
  }
}

const livebundle = new LiveBundle();
export default livebundle;
