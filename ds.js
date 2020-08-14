import {Dimensions, Platform} from 'react-native';
import DeviceInfo from 'react-native-device-info';

const h = Dimensions.get('window').height;
const w = Dimensions.get('window').width;
const hp = (percent) => (Dimensions.get('window').height / 100) * percent;
const wp = (percent) => (Dimensions.get('window').width / 100) * percent;
const isIphoneX =
  Platform.OS === 'ios' && (h === 812 || w === 812 || h === 896 || w === 896);
const isAndroid = Platform.OS === 'android';

const sD = h < 740;
const isIphoneXAndAbove = () => {
  const iphoneXAbove =
    +DeviceInfo.getDeviceId().replace('iPhone', '').replace(',', '.') >= 10;

  return (
    (DeviceInfo.hasNotch() && DeviceInfo.getModel() === 'Iphone') ||
    iphoneXAbove
  );
};

export default {
  h,
  w,
  hp,
  wp,
  sD,
  isIphoneX,
  isAndroid,
  isIphoneXAndAbove: isIphoneXAndAbove(),
};
