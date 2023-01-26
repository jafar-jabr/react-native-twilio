/*
export const TwilioView =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<TwilioProps>(ComponentName)
    : () => {
      throw new Error(LINKING_ERROR);
    };
*/

import {
  NativeEventEmitter,
  NativeModules,
  findNodeHandle,
  Platform,
  requireNativeComponent,
  UIManager,
  ViewStyle,
} from 'react-native';

import { Component, createRef } from 'react';

const twilioEmitter =
  Platform.OS === 'ios'
    ? new NativeEventEmitter(NativeModules.TwilioEmitter)
    : new NativeEventEmitter(NativeModules.TwilioModule);

export { RNTwilio, twilioEmitter };

export enum EventType {
  ON_FRAME_DIMENSIONS_CHANGED = 'onFrameDimensionsChanged',
  ON_CAMERA_SWITCHED = 'onCameraSwitched',
  ON_VIDEO_CHANGED = 'onVideoChanged',
  ON_AUDIO_CHANGED = 'onAudioChanged',
  ROOM_NAME = 'roomName',
  ROOM_SID = 'roomSid',
  PARTICIPANT_NAME = 'participantName',
  PARTICIPANT_SID = 'participantSid',
  PARTICIPANT = 'participant',
  PARTICIPANTS = 'participants',
  LOCAL_PARTICIPANT = 'localParticipant',
  TRACK = 'track',
  TRACK_SID = 'trackSid',
  TRACK_NAME = 'trackName',
  ENABLED = 'enabled',
  IDENTITY = 'identity',
  SID = 'sid',
  IS_LOCAL_USER = 'isLocalUser',
  QUALITY = 'quality',
  ERROR = 'error',
  END_CALL = 'endCall',
  ON_CONNECTED = 'onRoomDidConnect',
  ON_RE_CONNECTED = 'onRoomReConnect',
  ON_CONNECT_FAILURE = 'onRoomDidFailToConnect',
  ON_DISCONNECTED = 'onRoomDidDisconnect',
  ON_PARTICIPANT_CONNECTED = 'onRoomParticipantDidConnect',
  ON_PARTICIPANT_RECONNECTED = 'onRoomParticipantReconnect',
  ON_PARTICIPANT_DISCONNECTED = 'onRoomParticipantDidDisconnect',
  ON_DATATRACK_MESSAGE_RECEIVED = 'onDataTrackMessageReceived',
  ON_PARTICIPANT_ADDED_DATA_TRACK = 'onParticipantAddedDataTrack',
  ON_PARTICIPANT_REMOVED_DATA_TRACK = 'onParticipantRemovedDataTrack',
  ON_PARTICIPANT_ADDED_VIDEO_TRACK = 'onParticipantAddedVideoTrack',
  ON_PARTICIPANT_REMOVED_VIDEO_TRACK = 'onParticipantRemovedVideoTrack',
  ON_PARTICIPANT_ADDED_AUDIO_TRACK = 'onParticipantAddedAudioTrack',
  ON_PARTICIPANT_REMOVED_AUDIO_TRACK = 'onParticipantRemovedAudioTrack',
  ON_PARTICIPANT_ENABLED_VIDEO_TRACK = 'onParticipantEnabledVideoTrack',
  ON_PARTICIPANT_DISABLED_VIDEO_TRACK = 'onParticipantDisabledVideoTrack',
  ON_PARTICIPANT_ENABLED_AUDIO_TRACK = 'onParticipantEnabledAudioTrack',
  ON_PARTICIPANT_DISABLED_AUDIO_TRACK = 'onParticipantDisabledAudioTrack',
  ON_STATS_RECEIVED = 'onStatsReceived',
  ON_NETWORK_QUALITY_LEVELS_CHANGED = 'onNetworkQualityLevelsChanged',
  ON_DOMINANT_SPEAKER_CHANGED = 'onDominantSpeakerDidChange',
  ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS = 'onLocalParticipantSupportedCodecs',
  ON_VIDEO_ENABLED = 'onVideoEnabled',
  ON_MUTE = 'onAudioEnabled',
}

const LINKING_ERROR =
  `The package 'react-native-twillio' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

type TwilioProps = {
  src: {};
  style: ViewStyle;
};

const ComponentName = 'TwilioView';

const nativeEvents = {
  connectToRoom: 1,
  disconnect: 2,
  switchCamera: 3,
  toggleVideo: 4,
  toggleSound: 5,
  getStats: 6,
  disableOpenSLES: 7,
  toggleSoundSetup: 8,
  toggleRemoteSound: 9,
  releaseResource: 10,
  toggleBluetoothHeadset: 11,
  sendString: 12,
  publishVideo: 13,
  publishAudio: 14,
};
const RNTwilio =
  UIManager.getViewManagerConfig(ComponentName) != null
    ? requireNativeComponent<TwilioProps>(ComponentName)
    : () => {
        throw new Error(LINKING_ERROR);
      };
class TwilioView extends Component<TwilioProps> {
  private static ref: any = createRef();
  static runCommandAndroid(event: any, args: any) {
    UIManager.dispatchViewManagerCommand(
      findNodeHandle(this.ref.current),
      event,
      args
    );
  }
  static initialize() {
    const subscribeRegisterAndroid = TwilioView.listenTwilio();
    return () => {
      subscribeRegisterAndroid();
    };
  }
  static flipCamera() {
    if (Platform.OS === 'ios') {
      NativeModules.TwilioViewManager.switchCamera();
    } else {
      this.runCommandAndroid('switchCamera', []);
    }
  }
  static mute() {
    if (Platform.OS === 'ios') {
      NativeModules.TwilioViewManager.mute();
    } else {
      this.runCommandAndroid('mute', []);
    }
  }
  static closeCamera() {
    if (Platform.OS === 'ios') {
      NativeModules.TwilioViewManager.closeCamera();
    } else {
      this.runCommandAndroid('closeCamera', []);
    }
  }
  static endCall() {
    if (Platform.OS === 'ios') {
      NativeModules.TwilioViewManager.endCall();
    } else {
      this.runCommandAndroid('endCall', []);
    }
  }
  private static listenTwilio() {
    TwilioView.removeTwilioListeners();

    const subscriptions = [
      twilioEmitter.addListener(
        EventType.ON_CONNECT_FAILURE,
        ({ error }) => {}
      ),
      twilioEmitter.addListener(EventType.ON_RE_CONNECTED, ({ name }) => {}),
      twilioEmitter.addListener(EventType.ON_CONNECTED, ({ name }) => {}),
      twilioEmitter.addListener(EventType.ON_DISCONNECTED, ({ name }) => {}),
    ];
    return () => {
      subscriptions.map((subscription) => {
        subscription.remove();
      });
    };
  }

  private static removeTwilioListeners() {
    twilioEmitter.removeAllListeners(EventType.ON_CONNECTED);
    twilioEmitter.removeAllListeners(EventType.ON_DISCONNECTED);
    twilioEmitter.removeAllListeners(EventType.ON_CONNECT_FAILURE);
    twilioEmitter.removeAllListeners(EventType.ON_RE_CONNECTED);
  }

  render() {
    return <RNTwilio ref={TwilioView.ref} {...this.props} />;
  }
}
export default TwilioView;
