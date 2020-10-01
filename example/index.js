/**
 * @format
 */

import {AppRegistry, LogBox} from 'react-native';
import App from './App';
import {name as appName} from './app.json';
import livebundle from 'react-native-livebundle';

LogBox.ignoreAllLogs(true);

livebundle.initialize();

AppRegistry.registerComponent(appName, () => App);
