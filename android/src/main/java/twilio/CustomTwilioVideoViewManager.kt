package com.twilio

import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.twilio.src.Events
import com.twilio.src.NativeView

class CustomTwilioVideoViewManager : SimpleViewManager<NativeView?>() {
  override fun getName(): String {
    return REACT_CLASS
  }

  override fun createViewInstance(reactContext: ThemedReactContext): NativeView {
    val permissionAwareActivity = reactContext.currentActivity as PermissionAwareActivity?
    return reactContext.currentActivity?.let {
      NativeView(reactContext, true,
        it,permissionAwareActivity!!)
    }!!
  }

  @Deprecated("Deprecated in Java")
  override fun receiveCommand(view: NativeView, commandId: Int, args: ReadableArray?) {
    when (commandId) {
      CONNECT_TO_ROOM -> {
        val roomName = args!!.getString(0)
        val accessToken = args.getString(1)
        val enableAudio = args.getBoolean(2)
        val enableVideo = args.getBoolean(3)
        val enableRemoteAudio = args.getBoolean(4)
        val enableNetworkQualityReporting = args.getBoolean(5)
        val dominantSpeakerEnabled = args.getBoolean(6)
        val maintainVideoTrackInBackground = args.getBoolean(7)
        val cameraType = args.getString(8)
        val encodingParameters = args.getMap(9)
        val enableH264Codec =
          if (encodingParameters.hasKey("enableH264Codec")) encodingParameters.getBoolean("enableH264Codec") else false
        view.connectToRoomWrapper(
          roomName,
          accessToken,
          enableAudio,
          enableVideo,
          enableRemoteAudio,
          enableNetworkQualityReporting,
          dominantSpeakerEnabled,
          maintainVideoTrackInBackground,
          cameraType,
          enableH264Codec
        )
      }
      DISCONNECT -> view.disconnect()
      SWITCH_CAMERA -> view.switchCamera()
      TOGGLE_VIDEO -> {
        val videoEnabled = args!!.getBoolean(0)
        view.toggleVideo(videoEnabled)
      }
      TOGGLE_SOUND -> {
        val audioEnabled = args!!.getBoolean(0)
        view.toggleAudio(audioEnabled)
      }
      GET_STATS -> view.stats
      DISABLE_OPENSL_ES -> view.disableOpenSLES()
      TOGGLE_SOUND_SETUP -> {
        val speaker = args!!.getBoolean(0)
        view.toggleSoundSetup(speaker)
      }
      TOGGLE_REMOTE_SOUND -> {
        val remoteAudioEnabled = args!!.getBoolean(0)
        view.toggleRemoteAudio(remoteAudioEnabled)
      }
      RELEASE_RESOURCE -> view.releaseResource()
      TOGGLE_BLUETOOTH_HEADSET -> {
        val headsetEnabled = args!!.getBoolean(0)
       // view.toggleBluetoothHeadset(headsetEnabled)
      }
      SEND_STRING -> view.sendString(args!!.getString(0))
      PUBLISH_VIDEO -> view.publishLocalVideo(args!!.getBoolean(0))
      PUBLISH_AUDIO -> view.publishLocalAudio(args!!.getBoolean(0))
    }
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, MutableMap<String, String>>? {
    val map = MapBuilder.of(
     Events.ON_CAMERA_SWITCHED,
      MapBuilder.of("registrationName",Events.ON_CAMERA_SWITCHED),
     Events.ON_VIDEO_CHANGED,
      MapBuilder.of("registrationName",Events.ON_VIDEO_CHANGED),
     Events.ON_AUDIO_CHANGED,
      MapBuilder.of("registrationName",Events.ON_AUDIO_CHANGED),
     Events.ON_CONNECTED,
      MapBuilder.of("registrationName",Events.ON_CONNECTED),
     Events.ON_CONNECT_FAILURE,
      MapBuilder.of("registrationName",Events.ON_CONNECT_FAILURE),
     Events.ON_DISCONNECTED,
      MapBuilder.of("registrationName",Events.ON_DISCONNECTED),
     Events.ON_PARTICIPANT_CONNECTED,
      MapBuilder.of("registrationName",Events.ON_PARTICIPANT_CONNECTED)
    )
    map.putAll(
      MapBuilder.of(
       Events.ON_PARTICIPANT_DISCONNECTED,
        MapBuilder.of("registrationName",Events.ON_PARTICIPANT_DISCONNECTED),
       Events.ON_DATATRACK_MESSAGE_RECEIVED,
        MapBuilder.of(
          "registrationName",
         Events.ON_DATATRACK_MESSAGE_RECEIVED
        ),
       Events.ON_PARTICIPANT_ADDED_DATA_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_ADDED_DATA_TRACK
        ),
       Events.ON_PARTICIPANT_ADDED_VIDEO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_ADDED_VIDEO_TRACK
        ),
       Events.ON_PARTICIPANT_REMOVED_VIDEO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_REMOVED_VIDEO_TRACK
        ),
       Events.ON_PARTICIPANT_ADDED_AUDIO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_ADDED_AUDIO_TRACK
        ),
       Events.ON_PARTICIPANT_REMOVED_AUDIO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_REMOVED_AUDIO_TRACK
        )
      )
    )
    map.putAll(
      MapBuilder.of(
       Events.ON_PARTICIPANT_REMOVED_DATA_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_REMOVED_DATA_TRACK
        ),
       Events.ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS,
        MapBuilder.of(
          "registrationName",
         Events.ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS
        )
      )
    )
    map.putAll(
      MapBuilder.of(
       Events.ON_PARTICIPANT_ENABLED_VIDEO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_ENABLED_VIDEO_TRACK
        ),
       Events.ON_PARTICIPANT_DISABLED_VIDEO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_DISABLED_VIDEO_TRACK
        ),
       Events.ON_PARTICIPANT_ENABLED_AUDIO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_ENABLED_AUDIO_TRACK
        ),
       Events.ON_PARTICIPANT_DISABLED_AUDIO_TRACK,
        MapBuilder.of(
          "registrationName",
         Events.ON_PARTICIPANT_DISABLED_AUDIO_TRACK
        ),
       Events.ON_STATS_RECEIVED,
        MapBuilder.of("registrationName",Events.ON_STATS_RECEIVED),
       Events.ON_NETWORK_QUALITY_LEVELS_CHANGED,
        MapBuilder.of(
          "registrationName",
         Events.ON_NETWORK_QUALITY_LEVELS_CHANGED
        ),
       Events.ON_DOMINANT_SPEAKER_CHANGED,
        MapBuilder.of("registrationName",Events.ON_DOMINANT_SPEAKER_CHANGED)
      )
    )
    return map
  }

  override fun getCommandsMap(): Map<String, Int>? {
    return MapBuilder.builder<String, Int>()
      .put("connectToRoom", CONNECT_TO_ROOM)
      .put("disconnect", DISCONNECT)
      .put("switchCamera", SWITCH_CAMERA)
      .put("toggleVideo", TOGGLE_VIDEO)
      .put("toggleSound", TOGGLE_SOUND)
      .put("getStats", GET_STATS)
      .put("disableOpenSLES", DISABLE_OPENSL_ES)
      .put("toggleRemoteSound", TOGGLE_REMOTE_SOUND)
      .put("toggleBluetoothHeadset", TOGGLE_BLUETOOTH_HEADSET)
      .put("sendString", SEND_STRING)
      .build()
  }

  companion object {
    const val REACT_CLASS = "RNCustomTwilioVideoView"
    private const val CONNECT_TO_ROOM = 1
    private const val DISCONNECT = 2
    private const val SWITCH_CAMERA = 3
    private const val TOGGLE_VIDEO = 4
    private const val TOGGLE_SOUND = 5
    private const val GET_STATS = 6
    private const val DISABLE_OPENSL_ES = 7
    private const val TOGGLE_SOUND_SETUP = 8
    private const val TOGGLE_REMOTE_SOUND = 9
    private const val RELEASE_RESOURCE = 10
    private const val TOGGLE_BLUETOOTH_HEADSET = 11
    private const val SEND_STRING = 12
    private const val PUBLISH_VIDEO = 13
    private const val PUBLISH_AUDIO = 14
  }
}
