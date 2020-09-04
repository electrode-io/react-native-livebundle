import {setCustomSourceTransformer} from 'react-native/Libraries/Image/resolveAssetSource';
      setCustomSourceTransformer((resolver) => {
        const res = resolver.scaledAssetPath();
        const {hash, name, type} = resolver.asset;
        res.uri = `https://livebundle.blob.core.windows.net/demo/assets/${hash}/${name}.${type}`;
        return res;
      });
      /**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);
