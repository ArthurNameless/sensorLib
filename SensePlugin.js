import React, {useEffect} from 'react';
import {NativeModules, DeviceEventEmitter} from 'react-native';
import * as RNFS from 'react-native-fs';

const {ReactNativeSensor} = NativeModules;

const FACES_DETECTED_EVENT = 'FacesDetectedEvent';

export default ({onFacesDetected}) => {
  useEffect(() => {
    const dir = `${RNFS.CachesDirectoryPath}/Camera/`;
    console.log('ReactNativeSensor', ReactNativeSensor);
    ReactNativeSensor.init(dir);
    DeviceEventEmitter.removeAllListeners(FACES_DETECTED_EVENT);
    DeviceEventEmitter.addListener(FACES_DETECTED_EVENT, onFacesDetected);

    return () => {
      DeviceEventEmitter.removeAllListeners(FACES_DETECTED_EVENT);
    };
  }, [onFacesDetected]);

  return <></>;
};
