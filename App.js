/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, {useState} from 'react';
import {StyleSheet, Text, TouchableOpacity} from 'react-native';
import {RNCamera} from 'react-native-camera';
import ds from './ds';
import SensePlugin from './SensePlugin';

const App = () => {
  const detect = (obj) => {
    console.log('face detected', obj);
  };

  return (
    <>
      <Text style={styles.text}>Some text</Text>
      <SensePlugin onFacesDetected={detect} />
      {/* <RNCamera
        style={styles.camera}
        type={RNCamera.Constants.Type.front}
        flashMode={RNCamera.Constants.FlashMode.off}
        captureAudio={false}
        faceDetectionMode={RNCamera.Constants.FaceDetection.Mode.accurate}
        faceDetectionLandmarks={RNCamera.Constants.FaceDetection.Landmarks.all}
        captureFaces={false}
        onFacesDetected={({faces}) => {
          if (faces.length) {
            console.log('found some', faces);
          }
        }}
      /> */}
    </>
  );
};

const styles = StyleSheet.create({
  camera: {
    width: ds.w,
    height: ds.h - 40,
  },
  text: {
    fontSize: 20,
    textAlign: 'center',
    width: ds.w,
    height: 40,
    paddingTop: 10,
  },
});

export default App;
