package com.twilio.src

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.PreferenceManager
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.koushikdutta.ion.Ion
import com.twilio.R
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDevice.*
import com.twilio.audioswitch.AudioSwitch
import com.twilio.video.*
import com.twilio.video.VideoView
import com.twilio.video.ktx.Video.connect
import com.twilio.video.ktx.createLocalAudioTrack
import com.twilio.video.ktx.createLocalVideoTrack
import tvi.webrtc.VideoSink
import java.util.*
import kotlin.properties.Delegates

@SuppressLint("MissingInflatedId")
class NativeView(context: Context, reactApplicationContext: ReactApplicationContext, isFromReact: Boolean, activity: Activity, permissionAwareActivity:PermissionAwareActivity) :
  RelativeLayout(context) , PermissionListener, LifecycleOwner {

  private lateinit var lifecycleRegistry: LifecycleRegistry

  private val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
  private val TAG = "NativeViewTwilio"
  private val CAMERA_PERMISSION_INDEX = 0
  private val MIC_PERMISSION_INDEX = 1
  private val ACCESS_TOKEN_SERVER = ""
  //private val TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzdkNGI0NWZmYzU0OWQ2MjQ3ZmI1OGMwNmM3ZTdiMmU3LTE2NzMyMTc0MDkiLCJpc3MiOiJTSzdkNGI0NWZmYzU0OWQ2MjQ3ZmI1OGMwNmM3ZTdiMmU3Iiwic3ViIjoiQUNjNzc1OTc1ZTA3MDlkNTQ3OGFiN2Q2OTY2YjA0ODZkOCIsImV4cCI6MTY3MzIyMTAwOSwiZ3JhbnRzIjp7ImlkZW50aXR5IjoidXNlcjEzIiwidmlkZW8iOnsicm9vbSI6InJvb20xIn19fQ.yaJy2GIool_kmNjVDzwLCEt9ti8IplKCLAOEZDdNHps"
  private lateinit var accessToken: String
  public var myRoom: Room? = null
  public var myActivity: Activity? = null
  public var myPermissionAwareActivity: PermissionAwareActivity? = null
  public var myLocalParticipant: LocalParticipant? = null
  public var savedVolumeControlStream by Delegates.notNull<Int>()
  var localAudioTrack: LocalAudioTrack? = null
  var localVideoTrack: LocalVideoTrack? = null
  private var alertDialog: AlertDialog? = null
  var audioDeviceMenuItem: MenuItem? = null
  private var participantIdentity: String? = null
  lateinit var localVideoView: VideoSink
  var disconnectedFromOnDestroy = false
  private var isSpeakerPhoneEnabled = true
  var mainView: View? = null

  private var mReconnectingProgressBar: ProgressBar? = null
  private var mConnectActionFab: FloatingActionButton? = null
  private var mSwitchCameraActionFab: FloatingActionButton? = null
  private var mLocalVideoActionFab: FloatingActionButton? = null
  private var mMuteActionFab: FloatingActionButton? = null
  private var mThumbnailVideoView: VideoView? = null
  private var mPrimaryVideoView: VideoView? = null
  private var mReactApplicationContext: ReactApplicationContext? = null

  val cameraCapturerCompat by lazy {
    CameraCapturerCompat(this.context, CameraCapturerCompat.Source.FRONT_CAMERA)
  }

  val audioSwitch by lazy {
    AudioSwitch(
      this.context.applicationContext, preferredDeviceList = listOf(
        BluetoothHeadset::class.java,
        WiredHeadset::class.java, Speakerphone::class.java, Earpiece::class.java
      )
    )
  }

  init {
    mReactApplicationContext=reactApplicationContext
    val inflater = LayoutInflater.from(context)
    mainView = inflater.inflate(R.layout.activity_video, this)
    //-------------------------------------------------
    setAccessTokenFromPref()
    mReconnectingProgressBar =
      mainView!!.findViewById<View>(R.id.reconnectingProgressBar) as ProgressBar?
    mConnectActionFab =
      mainView!!.findViewById<View>(R.id.connectActionFab) as FloatingActionButton?
    mSwitchCameraActionFab =
      mainView!!.findViewById<View>(R.id.switchCameraActionFab) as FloatingActionButton?
    mLocalVideoActionFab =
      mainView!!.findViewById<View>(R.id.localVideoActionFab) as FloatingActionButton?
    mMuteActionFab = mainView!!.findViewById<View>(R.id.muteActionFab) as FloatingActionButton?
    mThumbnailVideoView = mainView!!.findViewById<View>(R.id.thumbnailVideoView) as VideoView?
    mPrimaryVideoView = mainView!!.findViewById<View>(R.id.primaryVideoView) as VideoView?
    mThumbnailVideoView!!.visibility= INVISIBLE


    //-------------------------------------------------

    myActivity = activity
    myPermissionAwareActivity = permissionAwareActivity
    localVideoView = mPrimaryVideoView!!

    savedVolumeControlStream = myActivity!!.volumeControlStream
    myActivity!!.volumeControlStream = AudioManager.STREAM_VOICE_CALL
    if (!checkPermissionForCameraAndMicrophone()) {
      requestPermissionForCameraMicrophoneAndBluetooth()
    } else {
      createAudioAndVideoTracks()
      audioSwitch.start { audioDevices, audioDevice -> updateAudioDeviceIcon(audioDevice) }
    }
    //-------------------------------------------------

    mReconnectingProgressBar =
      mainView!!.findViewById<View>(R.id.reconnectingProgressBar) as ProgressBar?
    mConnectActionFab =
      mainView!!.findViewById<View>(R.id.connectActionFab) as FloatingActionButton?
    mSwitchCameraActionFab =
      mainView!!.findViewById<View>(R.id.switchCameraActionFab) as FloatingActionButton?
    mLocalVideoActionFab =
      mainView!!.findViewById<View>(R.id.localVideoActionFab) as FloatingActionButton?
    mMuteActionFab = mainView!!.findViewById<View>(R.id.muteActionFab) as FloatingActionButton?
    mThumbnailVideoView = mainView!!.findViewById<View>(R.id.thumbnailVideoView) as VideoView?
    mPrimaryVideoView = mainView!!.findViewById<View>(R.id.primaryVideoView) as VideoView?

    //-------------------------------------------------
    localVideoTrack = if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
      createLocalVideoTrack(
        this.context,
        true,
        cameraCapturerCompat
      )
    } else {
      localVideoTrack
    }
    localVideoTrack?.addSink(localVideoView)
    localVideoTrack?.let { myLocalParticipant?.publishTrack(it) }
    myLocalParticipant?.setEncodingParameters(encodingParameters)

    myRoom?.let {
      mReconnectingProgressBar!!.visibility = if (it.state != Room.State.RECONNECTING)
        View.GONE else
        View.VISIBLE
    }
    initializeUI()
  }

  /*
   * AudioCodec and VideoCodec represent the preferred codec for encoding and decoding audio and
   * video.
   */
  private val audioCodec: AudioCodec
    get() {
      // TODO CHANGES ---------
      /*  val audioCodecName = sharedPreferences.getString(
          TwilioSettingsActivity.PREF_AUDIO_CODEC,
          TwilioSettingsActivity.PREF_AUDIO_CODEC_DEFAULT
        )*/
      val audioCodecName = OpusCodec.NAME
      return when (audioCodecName) {
        IsacCodec.NAME -> IsacCodec()
        OpusCodec.NAME -> OpusCodec()
        PcmaCodec.NAME -> PcmaCodec()
        PcmuCodec.NAME -> PcmuCodec()
        G722Codec.NAME -> G722Codec()
        else -> OpusCodec()
      }
    }
  private val videoCodec: VideoCodec
    get() {
      val videoCodecName = Vp8Codec.NAME
      return when (videoCodecName) {
        Vp8Codec.NAME -> {
          Vp8Codec(false)
        }
        H264Codec.NAME -> H264Codec()
        Vp9Codec.NAME -> Vp9Codec()
        else -> Vp8Codec()
      }
    }

  private val enableAutomaticSubscription: Boolean
    get() {
      return true
    }

  /*
   * Encoding parameters represent the sender side bandwidth constraints.
   */
  val encodingParameters: EncodingParameters
    get() {
      /* val defaultMaxAudioBitrate = TwilioSettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE_DEFAULT
       val defaultMaxVideoBitrate = TwilioSettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE_DEFAULT
       val maxAudioBitrate = Integer.parseInt(
         sharedPreferences.getString(
           TwilioSettingsActivity.PREF_SENDER_MAX_AUDIO_BITRATE,
           defaultMaxAudioBitrate
         ) ?: defaultMaxAudioBitrate
       )
       val maxVideoBitrate = Integer.parseInt(
         sharedPreferences.getString(
           TwilioSettingsActivity.PREF_SENDER_MAX_VIDEO_BITRATE,
           defaultMaxVideoBitrate
         ) ?: defaultMaxVideoBitrate
       )*/

      return EncodingParameters(2, 4)
    }

  /*
   * Room events listener
   */
  private val roomListener = object : Room.Listener {
    override fun onConnected(room: Room) {

      myLocalParticipant = room.localParticipant
      myActivity!!.title = room.name


      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())

      val participants = room.remoteParticipants
      val localParticipant = room.localParticipant
      localParticipant!!.setListener(localListener())
      val participantsArray: WritableArray = WritableNativeArray()
      for (participant in participants) {
        participantsArray.pushMap(buildParticipant(participant))
      }
      participantsArray.pushMap(buildParticipant(localParticipant))
      params.putArray(Constants.PARTICIPANTS, participantsArray)
      params.putMap(Constants.LOCAL_PARTICIPANT, buildParticipant(localParticipant))


      sendEvent(reactApplicationContext, Constants.ON_CONNECTED, params)

      // Only one participant is supported
      room.remoteParticipants.firstOrNull()?.let { addRemoteParticipant(it) }
      for (participant in participants) {
        addParticipant(room, participant!!)
      }

      room.remoteParticipants.firstOrNull()?.let { addRemoteParticipant(it) }
    }

    override fun onReconnected(room: Room) {

      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_CONNECTED, params)
      mReconnectingProgressBar!!.visibility = View.GONE
    }

    override fun onReconnecting(room: Room, twilioException: TwilioException) {
      mReconnectingProgressBar!!.visibility = View.VISIBLE
      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_RE_CONNECTED, params)

    }

    override fun onConnectFailure(room: Room, e: TwilioException) {
      audioSwitch.deactivate()
      val params = Arguments.createMap()
      params.putString(Constants.ERROR, e.toString())
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_CONNECT_FAILURE, params)
      initializeUI()
    }

    override fun onDisconnected(room: Room, e: TwilioException?) {
      myLocalParticipant = null
      mReconnectingProgressBar!!.visibility = View.GONE
      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_DISCONNECTED, params)

      myRoom = null
      // Only reinitialize the UI if disconnect was not called from onDestroy()
      if (!disconnectedFromOnDestroy) {
        audioSwitch.deactivate()
        initializeUI()
        moveLocalVideoToPrimaryView()
      }
    }

    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
      Log.d(TAG, "got this al leat")
      addRemoteParticipant(participant)
    }

    override fun onParticipantReconnected(room: Room, participant: RemoteParticipant) {
      Log.d(TAG, "got this al leat then")
      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      params.putString(Constants.PARTICIPANT_SID, participant.sid)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_RECONNECTED, params)

      addRemoteParticipant(participant)
    }

    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
      removeRemoteParticipant(participant)
    }

    override fun onDominantSpeakerChanged(room: Room, remoteParticipant: RemoteParticipant?) {
      val event: WritableMap = WritableNativeMap()

      event.putString("roomName", room.name)
      event.putString("roomSid", room.sid)

      if (remoteParticipant == null) {
        event.putString("participant", "")
      } else {
        event.putMap("participant", buildParticipant(remoteParticipant))
      }

      sendEvent(reactApplicationContext, Constants.ON_DOMINANT_SPEAKER_CHANGED, event)

    }

    override fun onRecordingStarted(room: Room) {
      /*
       * Indicates when media shared to a Room is being recorded. Note that
       * recording is only available in our Group Rooms developer preview.
       */
      Log.d(TAG, "onRecordingStarted")
    }

    override fun onRecordingStopped(room: Room) {
      /*
       * Indicates when media shared to a Room is no longer being recorded. Note that
       * recording is only available in our Group Rooms developer preview.
       */
      Log.d(TAG, "onRecordingStopped")
    }
  }

  private fun localListener(): LocalParticipant.Listener {
    return object : LocalParticipant.Listener {
      override fun onAudioTrackPublished(
        localParticipant: LocalParticipant,
        localAudioTrackPublication: LocalAudioTrackPublication
      ) {
      }

      override fun onAudioTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localAudioTrack: LocalAudioTrack,
        twilioException: TwilioException
      ) {
      }

      override fun onVideoTrackPublished(
        localParticipant: LocalParticipant,
        localVideoTrackPublication: LocalVideoTrackPublication
      ) {
      }

      override fun onVideoTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localVideoTrack: LocalVideoTrack,
        twilioException: TwilioException
      ) {
      }

      override fun onDataTrackPublished(
        localParticipant: LocalParticipant,
        localDataTrackPublication: LocalDataTrackPublication
      ) {
      }

      override fun onDataTrackPublicationFailed(
        localParticipant: LocalParticipant,
        localDataTrack: LocalDataTrack,
        twilioException: TwilioException
      ) {
      }

      override fun onNetworkQualityLevelChanged(
        localParticipant: LocalParticipant,
        networkQualityLevel: NetworkQualityLevel
      ) {
        val event: WritableMap = WritableNativeMap()
        event.putMap(Constants.PARTICIPANT, buildParticipant(localParticipant))
        event.putBoolean(Constants.IS_LOCAL_USER, true)
        // Twilio SDK defines Enum 0 as UNKNOWN and 1 as Quality ZERO, so we subtract one to get the correct quality level as an integer
        event.putInt(Constants.QUALITY, networkQualityLevel.ordinal - 1)
        sendEvent(mReactApplicationContext!!, Constants.ON_NETWORK_QUALITY_LEVELS_CHANGED, event)
      }
    }
  }

  private fun buildParticipant(participant: Participant): WritableMap {
    val participantMap: WritableMap = WritableNativeMap()
    participantMap.putString(Constants.IDENTITY, participant.identity)
    participantMap.putString(Constants.SID, participant.sid)
    return participantMap
  }
  private fun buildTrack(publication: TrackPublication): WritableMap? {
    val trackMap: WritableMap = WritableNativeMap()
    trackMap.putString(Constants.TRACK_SID, publication.trackSid)
    trackMap.putString(Constants.TRACK_NAME, publication.trackName)
    trackMap.putBoolean(Constants.ENABLED, publication.isTrackEnabled)
    return trackMap
  }

  private fun buildRemoveParticipantVideo(
    participant: Participant,
    deleteVideoTrack: RemoteVideoTrackPublication
  ) {
    val event = buildParticipantVideoEvent(participant, deleteVideoTrack)
    sendEvent(mReactApplicationContext!!, Constants.ON_PARTICIPANT_REMOVED_VIDEO_TRACK, event)
  }
  private fun addParticipant(room: Room, remoteParticipant: RemoteParticipant) {
    val event: WritableMap = WritableNativeMap()
    event.putString("roomName", room.name)
    event.putString("roomSid", room.sid)
    event.putMap("participant", buildParticipant(remoteParticipant))
    sendEvent(mReactApplicationContext!!, Constants.ON_PARTICIPANT_CONNECTED, event)
  }
  private fun buildParticipantDataEvent(
    participant: Participant,
    publication: TrackPublication
  ): WritableMap {
    val participantMap: WritableMap = buildParticipant(participant)!!
    val trackMap: WritableMap = buildTrack(publication)!!
    val event: WritableMap = WritableNativeMap()
    event.putMap(Constants.PARTICIPANT, participantMap)
    event.putMap(Constants.TRACK, trackMap)
    return event
  }

  private fun buildParticipantVideoEvent(
    participant: Participant,
    publication: TrackPublication
  ): WritableMap? {
    val participantMap = buildParticipant(participant)
    val trackMap: WritableMap = buildTrack(publication)!!
    val event: WritableMap = WritableNativeMap()
    event.putMap("participant", participantMap)
    event.putMap("track", trackMap)
    return event
  }

  private fun addParticipantVideoEvent(
    participant: Participant,
    publication: RemoteVideoTrackPublication
  ) {
    val event: WritableMap = buildParticipantVideoEvent(participant, publication)!!
    sendEvent(mReactApplicationContext!!, Constants.ON_PARTICIPANT_ADDED_VIDEO_TRACK, event)
  }
  /*
   * RemoteParticipant events listener
   */
  private val participantListener = object : RemoteParticipant.Listener {
    override fun onAudioTrackPublished(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
      Log.i(
        TAG, "onAudioTrackPublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
          "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
          "name=${remoteAudioTrackPublication.trackName}]"
      )
    }

    override fun onAudioTrackUnpublished(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
      Log.i(
        TAG, "onAudioTrackUnpublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
          "enabled=${remoteAudioTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteAudioTrackPublication.isTrackSubscribed}, " +
          "name=${remoteAudioTrackPublication.trackName}]"
      )
    }

    override fun onDataTrackPublished(
      remoteParticipant: RemoteParticipant,
      remoteDataTrackPublication: RemoteDataTrackPublication
    ) {
      Log.i(
        TAG, "onDataTrackPublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
          "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
          "name=${remoteDataTrackPublication.trackName}]"
      )
    }

    override fun onDataTrackUnpublished(
      remoteParticipant: RemoteParticipant,
      remoteDataTrackPublication: RemoteDataTrackPublication
    ) {
      Log.i(
        TAG, "onDataTrackUnpublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
          "enabled=${remoteDataTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteDataTrackPublication.isTrackSubscribed}, " +
          "name=${remoteDataTrackPublication.trackName}]"
      )
    }

    override fun onVideoTrackPublished(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
      Log.i(
        TAG, "onVideoTrackPublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
          "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
          "name=${remoteVideoTrackPublication.trackName}]"
      )
    }

    override fun onVideoTrackUnpublished(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
      Log.i(
        TAG, "onVideoTrackUnpublished: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
          "enabled=${remoteVideoTrackPublication.isTrackEnabled}, " +
          "subscribed=${remoteVideoTrackPublication.isTrackSubscribed}, " +
          "name=${remoteVideoTrackPublication.trackName}]"
      )
    }

    override fun onAudioTrackSubscribed(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication,
      remoteAudioTrack: RemoteAudioTrack
    ) {
      Log.i(
        TAG, "onAudioTrackSubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
          "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
          "name=${remoteAudioTrack.name}]"
      )
    }

    override fun onAudioTrackUnsubscribed(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication,
      remoteAudioTrack: RemoteAudioTrack
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteAudioTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_REMOVED_AUDIO_TRACK, event)

      Log.i(
        TAG, "onAudioTrackUnsubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteAudioTrack: enabled=${remoteAudioTrack.isEnabled}, " +
          "playbackEnabled=${remoteAudioTrack.isPlaybackEnabled}, " +
          "name=${remoteAudioTrack.name}]"
      )
    }

    override fun onAudioTrackSubscriptionFailed(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication,
      twilioException: TwilioException
    ) {
      Log.i(
        TAG, "onAudioTrackSubscriptionFailed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteAudioTrackPublication: sid=${remoteAudioTrackPublication.trackSid}, " +
          "name=${remoteAudioTrackPublication.trackName}]" +
          "[TwilioException: code=${twilioException.code}, " +
          "message=${twilioException.message}]"
      )
    }

    override fun onDataTrackSubscribed(
      remoteParticipant: RemoteParticipant,
      remoteDataTrackPublication: RemoteDataTrackPublication,
      remoteDataTrack: RemoteDataTrack
    ) {
      val event: WritableMap =
        buildParticipantDataEvent(remoteParticipant, remoteDataTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_ADDED_DATA_TRACK, event)

      Log.i(
        TAG, "onDataTrackSubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
          "name=${remoteDataTrack.name}]"
      )
    }
    override fun onDataTrackUnsubscribed(
      remoteParticipant: RemoteParticipant,
      remoteDataTrackPublication: RemoteDataTrackPublication,
      remoteDataTrack: RemoteDataTrack
    ) {
      val event: WritableMap =
        buildParticipantDataEvent(remoteParticipant, remoteDataTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_REMOVED_DATA_TRACK, event)

      Log.i(
        TAG, "onDataTrackUnsubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteDataTrack: enabled=${remoteDataTrack.isEnabled}, " +
          "name=${remoteDataTrack.name}]"
      )
    }

    override fun onDataTrackSubscriptionFailed(
      remoteParticipant: RemoteParticipant,
      remoteDataTrackPublication: RemoteDataTrackPublication,
      twilioException: TwilioException
    ) {
      Log.i(
        TAG, "onDataTrackSubscriptionFailed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteDataTrackPublication: sid=${remoteDataTrackPublication.trackSid}, " +
          "name=${remoteDataTrackPublication.trackName}]" +
          "[TwilioException: code=${twilioException.code}, " +
          "message=${twilioException.message}]"
      )
    }

    override fun onVideoTrackSubscribed(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication,
      remoteVideoTrack: RemoteVideoTrack
    ) {
      Log.i(
        TAG, "onVideoTrackSubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
          "name=${remoteVideoTrack.name}]"
      )
      addParticipantVideoEvent(remoteParticipant, remoteVideoTrackPublication)
      addRemoteParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackUnsubscribed(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication,
      remoteVideoTrack: RemoteVideoTrack
    ) {
      Log.i(
        TAG, "onVideoTrackUnsubscribed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrack: enabled=${remoteVideoTrack.isEnabled}, " +
          "name=${remoteVideoTrack.name}]"
      )
      buildRemoveParticipantVideo(remoteParticipant, remoteVideoTrackPublication)
      removeParticipantVideo(remoteVideoTrack)
    }

    override fun onVideoTrackSubscriptionFailed(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication,
      twilioException: TwilioException
    ) {
      Log.i(
        TAG, "onVideoTrackSubscriptionFailed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
          "name=${remoteVideoTrackPublication.trackName}]" +
          "[TwilioException: code=${twilioException.code}, " +
          "message=${twilioException.message}]"
      )
      Snackbar.make(
        mConnectActionFab!!,
        "Failed to subscribe to ${remoteParticipant.identity}",
        Snackbar.LENGTH_LONG
      )
        .show()
    }

    override fun onAudioTrackEnabled(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteAudioTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_ENABLED_AUDIO_TRACK, event)

    }

    override fun onVideoTrackEnabled(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteVideoTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_ENABLED_VIDEO_TRACK, event)

    }

    override fun onVideoTrackDisabled(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteVideoTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_DISABLED_VIDEO_TRACK, event)

    }

    override fun onAudioTrackDisabled(
      remoteParticipant: RemoteParticipant,
      remoteAudioTrackPublication: RemoteAudioTrackPublication
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteAudioTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_DISABLED_AUDIO_TRACK, event)

    }
  }

  private val sharedPreferences by lazy {
    PreferenceManager.getDefaultSharedPreferences(this.context)
  }


  private fun checkPermissions(permissions: Array<String>): Boolean {
    var shouldCheck = true
    for (permission in permissions) {
      shouldCheck = shouldCheck and (PackageManager.PERMISSION_GRANTED ==
        ContextCompat.checkSelfPermission(this.context, permission))
    }
    return shouldCheck
  }

  private fun requestPermissions(permissions: Array<String>) {
    var displayRational = false
    for (permission in permissions) {
      displayRational =
        displayRational or ActivityCompat.shouldShowRequestPermissionRationale(
          myActivity!!,
          permission
        )
    }
    if (displayRational) {
      Toast.makeText(this.context, R.string.permissions_needed, Toast.LENGTH_LONG).show()
    } else {
      if (myPermissionAwareActivity != null) {
        myPermissionAwareActivity?.requestPermissions(
          permissions,
          CAMERA_MIC_PERMISSION_REQUEST_CODE,
          this
        )
      }
    }
  }

  fun checkPermissionForCameraAndMicrophone(): Boolean {
    return checkPermissions(
      arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    )
  }

  fun requestPermissionForCameraMicrophoneAndBluetooth() {
    val permissionsList: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH_CONNECT
      )
    } else {
      arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
      )
    }
    requestPermissions(permissionsList)
  }

  fun createAudioAndVideoTracks() {
    // Share your microphone
    localAudioTrack = createLocalAudioTrack(this.context, true)

    // Share your camera
    localVideoTrack = createLocalVideoTrack(
      this.context,
      true,
      cameraCapturerCompat
    )
  }

  public fun setAccessTokenFromPref() {
    val sharedPref = mReactApplicationContext!!
      .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    val prefToken = sharedPref.getString(Constants.PREF_TOEKN, null)
    this.accessToken= prefToken.toString();

  }

  fun connectToRoom(src: ReadableMap) {
    setAccessTokenFromPref()
    if( this.accessToken==null|| this.accessToken.isEmpty()){
      this.accessToken = src.getString("token")!!
    }
    if(this.accessToken==null) return
    if(this.accessToken.isEmpty()) return
    try {
      audioSwitch.activate()
      Log.d(TAG, "On connectToRoom =========================\n" +
        " roomName:${src.getString("roomName")!!}\n")

      myRoom = connect(this.context, accessToken, roomListener) {
        roomName(src.getString("roomName")!!)
        /*
         * Add local audio track to connect options to share with participants.
         */
        audioTracks(listOf(localAudioTrack))
        /*
         * Add local video track to connect options to share with participants.
         */
        videoTracks(listOf(localVideoTrack))

        /*
         * Set the preferred audio and video codec for media.
         */
        preferAudioCodecs(listOf(audioCodec))
        preferVideoCodecs(listOf(videoCodec))

        /*
         * Set the sender side encoding parameters.
         */
        encodingParameters(encodingParameters)

        /*
         * Toggles automatic track subscription. If set to false, the LocalParticipant will receive
         * notifications of track publish events, but will not automatically subscribe to them. If
         * set to true, the LocalParticipant will automatically subscribe to tracks as they are
         * published. If unset, the default is true. Note: This feature is only available for Group
         * Rooms. Toggling the flag in a P2P room does not modify subscription behavior.
         */
        enableAutomaticSubscription(enableAutomaticSubscription)
      }
      setDisconnectAction()
    }catch (e:Exception){
      Log.d(TAG, "On connect Exception ${e.toString()}")
    }
  }

  /*
   * The initial state when there is no active room.
   */
  fun initializeUI() {


    mConnectActionFab!!.hide()
   // mConnectActionFab!!.setOnClickListener(connectActionClickListener())
    mSwitchCameraActionFab!!.show()
    mSwitchCameraActionFab!!.setOnClickListener(switchCameraClickListener())
    mLocalVideoActionFab!!.show()
    mLocalVideoActionFab!!.setOnClickListener(localVideoClickListener())
    mMuteActionFab!!.show()
    mMuteActionFab!!.setOnClickListener(muteClickListener())
  }

  /*
   * Show the current available audio devices.
   */
  fun showAudioDevices() {
    val availableAudioDevices = audioSwitch.availableAudioDevices

    audioSwitch.selectedAudioDevice?.let { selectedDevice ->
      val selectedDeviceIndex = availableAudioDevices.indexOf(selectedDevice)
      val audioDeviceNames = ArrayList<String>()

      for (a in availableAudioDevices) {
        audioDeviceNames.add(a.name)
      }

      AlertDialog.Builder(this.context)
        .setTitle(R.string.room_screen_select_device)
        .setSingleChoiceItems(
          audioDeviceNames.toTypedArray<CharSequence>(),
          selectedDeviceIndex
        ) { dialog, index ->
          dialog.dismiss()
          val selectedAudioDevice = availableAudioDevices[index]
          updateAudioDeviceIcon(selectedAudioDevice)
          audioSwitch.selectDevice(selectedAudioDevice)
        }.create().show()
    }
  }

  /*
   * Update the menu icon based on the currently selected audio device.
   */
  infix fun updateAudioDeviceIcon(selectedAudioDevice: AudioDevice?) {
    val audioDeviceMenuIcon = when (selectedAudioDevice) {
      is BluetoothHeadset -> R.drawable.ic_bluetooth_white_24dp
      is WiredHeadset -> R.drawable.ic_headset_mic_white_24dp
      is Speakerphone -> R.drawable.ic_volume_up_white_24dp
      else -> R.drawable.ic_phonelink_ring_white_24dp
    }

    audioDeviceMenuItem?.setIcon(audioDeviceMenuIcon)
  }

  /*
   * The actions performed during disconnect.
   */
  private fun setDisconnectAction() {
    mConnectActionFab!!.setImageDrawable(
      ContextCompat.getDrawable(
        this.context,
        R.drawable.ic_call_end_white_24px
      )
    )
    mConnectActionFab!!.show()
    mConnectActionFab!!.setOnClickListener(disconnectClickListener())
  }

  /*
   * Creates an connect UI dialog
   */
  private fun showConnectDialog() {
    val tokenEditText = EditText(this.context)
    val roomEditText = EditText(this.context)

    alertDialog = createConnectDialog(
      tokenEditText,
      roomEditText,
      connectClickListener(tokenEditText,roomEditText), cancelConnectDialogClickListener(), this.context
    )
    alertDialog?.show()
  }

  /*
   * Called when participant joins the room
   */
  private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
    /*
     * This app only displays video for one additional participant per Room
     */

    if (mThumbnailVideoView!!.visibility == View.VISIBLE) {
      Snackbar.make(
        mConnectActionFab!!,
        "Multiple participants are not currently support in this UI",
        Snackbar.LENGTH_LONG
      )
        .setAction("Action", null).show()
      return
    }
    participantIdentity = remoteParticipant.identity

    /*
     * Add participant renderer
     */
    remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
      if (remoteVideoTrackPublication.isTrackSubscribed) {
        remoteVideoTrackPublication.remoteVideoTrack?.let { addRemoteParticipantVideo(it) }
      } else {
        Log.i(
          TAG, "wow in the else: " + remoteVideoTrackPublication.isTrackSubscribed
        )
      }
    }

    /*
     * Start listening for participant events
     */
    remoteParticipant.setListener(participantListener)
  }

  /*
   * Set primary view as renderer for participant video track
   */
  private fun addRemoteParticipantVideo(videoTrack: VideoTrack) {
    moveLocalVideoToThumbnailView()
    mPrimaryVideoView!!.mirror = false
    videoTrack.addSink(mPrimaryVideoView!!)
  }

  private fun moveLocalVideoToThumbnailView() {
    Log.i(
      TAG, "moveLocalVideoToThumbnailView"
    )
    if (mThumbnailVideoView!!.visibility == View.INVISIBLE) {
      Log.i(
        TAG, "moveLocalVideoToThumbnailView ==== 1"
      )
      mThumbnailVideoView!!.visibility = View.VISIBLE
      with(localVideoTrack) {
        this?.removeSink(mPrimaryVideoView!!)
        this?.addSink(mThumbnailVideoView!!)
      }
      localVideoView = mThumbnailVideoView!!
      mThumbnailVideoView!!.mirror = cameraCapturerCompat.cameraSource ==
        CameraCapturerCompat.Source.FRONT_CAMERA
    }
  }

  /*
   * Called when participant leaves the room
   */
  private fun removeRemoteParticipant(remoteParticipant: RemoteParticipant) {
    if (remoteParticipant.identity != participantIdentity) {
      return
    }

    /*
     * Remove participant renderer
     */
    remoteParticipant.remoteVideoTracks.firstOrNull()?.let { remoteVideoTrackPublication ->
      if (remoteVideoTrackPublication.isTrackSubscribed) {
        remoteVideoTrackPublication.remoteVideoTrack?.let { removeParticipantVideo(it) }
      }
    }
    moveLocalVideoToPrimaryView()
  }

  private fun removeParticipantVideo(videoTrack: VideoTrack) {
    videoTrack.removeSink(mPrimaryVideoView!!)
  }

  private fun moveLocalVideoToPrimaryView() {
    Log.i(
      TAG, "moveLocalVideoToThumbnailView"
    )
    if (mThumbnailVideoView!!.visibility == View.VISIBLE) {
      mThumbnailVideoView!!.visibility = View.INVISIBLE
      with(localVideoTrack) {
        this?.removeSink(mThumbnailVideoView!!)
        this?.addSink(mPrimaryVideoView!!)
      }
      localVideoView = mPrimaryVideoView!!
      mPrimaryVideoView!!.mirror = cameraCapturerCompat.cameraSource ==
        CameraCapturerCompat.Source.FRONT_CAMERA
    }
  }

  private fun connectClickListener(tokenEditText: EditText,roomEditText: EditText): DialogInterface.OnClickListener {
    return DialogInterface.OnClickListener { _, _ ->
      /*
       * Connect to room
       */
     // setAccessToken(tokenEditText.text.toString())
      //connectToRoom(roomEditText.text.toString())
    }
  }

  private fun disconnectClickListener(): View.OnClickListener {
    return View.OnClickListener {
      /*
       * Disconnect from room
       */
      val event: WritableMap = WritableNativeMap()
      event.putString(Constants.END_CALL, Constants.END_CALL)
      sendEvent(mReactApplicationContext!!, Constants.END_CALL, event)
      myRoom?.disconnect()
      mConnectActionFab!!.hide()
      initializeUI()
    }
  }

  private fun connectActionClickListener(): View.OnClickListener {
    return View.OnClickListener { showConnectDialog() }
  }

  private fun cancelConnectDialogClickListener(): DialogInterface.OnClickListener {
    return DialogInterface.OnClickListener { _, _ ->
      initializeUI()
      alertDialog?.dismiss()
    }
  }

  private fun switchCameraClickListener(): View.OnClickListener {
    return View.OnClickListener {
      val cameraSource = cameraCapturerCompat.cameraSource
      cameraCapturerCompat.switchCamera()
      if (mPrimaryVideoView!!.visibility == View.VISIBLE) {
        mPrimaryVideoView!!.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
      } else {
        mPrimaryVideoView!!.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
      }
    }
  }

  private fun localVideoClickListener(): View.OnClickListener {
    return View.OnClickListener {
      /*
       * Enable/disable the local video track
       */
      localVideoTrack?.let {
        val enable = !it.isEnabled
        it.enable(enable)
        val icon: Int
        if (enable) {
          icon = R.drawable.ic_videocam_white_24dp
          mSwitchCameraActionFab!!.show()
        } else {
          icon = R.drawable.ic_videocam_off_black_24dp
          mSwitchCameraActionFab!!.hide()
        }
        mLocalVideoActionFab!!.setImageDrawable(
          ContextCompat.getDrawable(this.context, icon)
        )
      }
    }
  }

  private fun muteClickListener(): View.OnClickListener {
    return View.OnClickListener {
      /*
       * Enable/disable the local audio track. The results of this operation are
       * signaled to other Participants in the same Room. When an audio track is
       * disabled, the audio is muted.
       */

      localAudioTrack?.let {
        val enable = !it.isEnabled
        it.enable(enable)
        val icon = if (enable)
          R.drawable.ic_mic_white_24dp
        else
          R.drawable.ic_mic_off_black_24dp
        mMuteActionFab!!.setImageDrawable(
          ContextCompat.getDrawable(
            this.context, icon
          )
        )
      }
    }
  }

  private fun retrieveAccessTokenfromServer() {
    Ion.with(this.context)
      .load("$ACCESS_TOKEN_SERVER?identity=${UUID.randomUUID()}")
      .asString()
      .setCallback { e, token ->
        if (e == null) {
          this.accessToken = token
        } else {
          Toast.makeText(
            this.context,
            R.string.error_retrieving_access_token, Toast.LENGTH_LONG
          )
            .show()
        }
      }
  }

  private fun createConnectDialog(
    tokenEditText: EditText,
    participantEditText: EditText,
    callParticipantsClickListener: DialogInterface.OnClickListener,
    cancelClickListener: DialogInterface.OnClickListener,
    context: Context
  ): AlertDialog {
    val alertDialogBuilder = AlertDialog.Builder(context).apply {
      setIcon(R.drawable.ic_video_call_white_24dp)
      setTitle("Connect to a room")
      setPositiveButton("Connect", callParticipantsClickListener)
      setNegativeButton("Cancel", cancelClickListener)
      setCancelable(false)
    }

    setRoomNameFieldInDialog(tokenEditText,participantEditText, alertDialogBuilder, context)

    return alertDialogBuilder.create()
  }

  @SuppressLint("RestrictedApi")
  private fun setRoomNameFieldInDialog(
    tokenEditText: EditText,
    roomNameEditText: EditText,
    alertDialogBuilder: AlertDialog.Builder,
    context: Context
  ) {
    roomNameEditText.hint = "room name"
    tokenEditText.hint = "token"
    val horizontalPadding =
      context.resources.getDimensionPixelOffset(R.dimen.activity_horizontal_margin)
    val verticalPadding =
      context.resources.getDimensionPixelOffset(R.dimen.activity_vertical_margin)
    val ll = LinearLayout(context)
    ll.orientation = LinearLayout.VERTICAL
    ll.addView(tokenEditText)
    ll.addView(roomNameEditText)
    alertDialogBuilder.setView(
      ll,
      horizontalPadding,
      verticalPadding,
      horizontalPadding,
      0
    )
  }

  fun requestPermissions(
    grantResults: IntArray
  ) {
    /*
     * The first two permissions are Camera & Microphone, bluetooth isn't required but
     * enabling it enables bluetooth audio routing functionality.
     */
    val cameraAndMicPermissionGranted =
      ((PackageManager.PERMISSION_GRANTED == grantResults[CAMERA_PERMISSION_INDEX])
        and (PackageManager.PERMISSION_GRANTED == grantResults[MIC_PERMISSION_INDEX]))

    /*
     * Due to bluetooth permissions being requested at the same time as camera and mic
     * permissions, AudioSwitch should be started after providing the user the option
     * to grant the necessary permissions for bluetooth.
     */
    audioSwitch.start { audioDevices, audioDevice -> updateAudioDeviceIcon(audioDevice) }

    if (cameraAndMicPermissionGranted) {
      createAudioAndVideoTracks()
    } else {
      Toast.makeText(
        this.myActivity,
        R.string.permissions_needed,
        Toast.LENGTH_LONG
      ).show()
    }

  }

  override fun getLifecycle(): Lifecycle {
    Log.d(TAG, "lifecycleRegistry=${lifecycleRegistry.currentState}")
    return lifecycleRegistry

  }
  override fun onStartTemporaryDetach() {
    super.onStartTemporaryDetach()

    Log.d(TAG, "onStartTemporaryDetach")
  }

  override fun onAttachedToWindow() {
    Log.d(TAG, "onAttachedToWindow")

    super.onAttachedToWindow()
  }

  override fun onDetachedFromWindow() {
    Log.d(TAG, "onAttachedToWindow")

    super.onDetachedFromWindow()
  }

  override fun onFinishInflate() {
    Log.d(TAG, "onFinishInflate")

    super.onFinishInflate()
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    Log.d(TAG, "onLayout")

    super.onLayout(changed, l, t, r, b)
  }

  override fun onViewAdded(child: View?) {
    Log.d(TAG, "onViewAdded")
    super.onViewAdded(child)
  }

  override fun onSaveInstanceState(): Parcelable? {
    Log.d(TAG, "onSaveInstanceState")
    return super.onSaveInstanceState()
  }
  fun onPause(owner: LifecycleOwner) {
    localVideoTrack?.let { myLocalParticipant?.unpublishTrack(it) }
    localVideoTrack?.release()
    localVideoTrack = null
  }

  fun onDestroy(owner: LifecycleOwner) {
    audioSwitch.stop()
    myActivity!!.volumeControlStream = savedVolumeControlStream
    myRoom?.disconnect()
    disconnectedFromOnDestroy = true
    localAudioTrack?.release()
    localVideoTrack?.release()
  }


  fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = myActivity!!.menuInflater
    inflater.inflate(R.menu.menu, menu)
    audioDeviceMenuItem = menu.findItem(R.id.menu_audio_device)

    // AudioSwitch has already started and thus notified of the initial selected device
    // so we need to updates the UI
    updateAudioDeviceIcon(audioSwitch.selectedAudioDevice)
    return true
  }


  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>?,
    grantResults: IntArray?
  ): Boolean {
    if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE) {
      /*
       * The first two permissions are Camera & Microphone, bluetooth isn't required but
       * enabling it enables bluetooth audio routing functionality.
       */
      val cameraAndMicPermissionGranted =
        ((PackageManager.PERMISSION_GRANTED == grantResults!![CAMERA_PERMISSION_INDEX])
          and (PackageManager.PERMISSION_GRANTED == grantResults[MIC_PERMISSION_INDEX]))

      /*
       * Due to bluetooth permissions being requested at the same time as camera and mic
       * permissions, AudioSwitch should be started after providing the user the option
       * to grant the necessary permissions for bluetooth.
       */
      audioSwitch.start { audioDevices, audioDevice -> updateAudioDeviceIcon(audioDevice) }

      if (cameraAndMicPermissionGranted) {
        createAudioAndVideoTracks()
        audioSwitch.start { audioDevices, audioDevice -> updateAudioDeviceIcon(audioDevice) }

      } else {
        Toast.makeText(
          this.context,
          R.string.permissions_needed,
          Toast.LENGTH_LONG
        ).show()
      }
    }
    return ((PackageManager.PERMISSION_GRANTED == grantResults!![CAMERA_PERMISSION_INDEX])
      and (PackageManager.PERMISSION_GRANTED == grantResults[MIC_PERMISSION_INDEX]))
  }


  private fun sendEvent(
    reactContext: ReactContext,
    eventName: String,
    params: WritableMap?
  ) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
}
