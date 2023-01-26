package com.twilio.src

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.twilio.R
import com.twilio.audioswitch.AudioDevice.*
import com.twilio.audioswitch.AudioSwitch
import com.twilio.src.utils.isH264Supported
import com.twilio.video.*
import com.twilio.video.VideoView
import com.twilio.video.ktx.Video.connect
import com.twilio.video.ktx.createLocalAudioTrack
import com.twilio.video.ktx.createLocalVideoTrack
import tvi.webrtc.VideoSink
import java.util.*
import kotlin.properties.Delegates


@SuppressLint("MissingInflatedId")
class NativeView(
  context: Context,
  reactApplicationContext: ReactApplicationContext,
  isFromReact: Boolean,
  activity: Activity,
  permissionAwareActivity: PermissionAwareActivity
) :
  RelativeLayout(context), PermissionListener, LifecycleOwner {

  private lateinit var lifecycleRegistry: LifecycleRegistry

  private val CAMERA_MIC_PERMISSION_REQUEST_CODE = 1
  private val TAG = "NativeViewTwilio"
  private val CAMERA_PERMISSION_INDEX = 0
  private val MIC_PERMISSION_INDEX = 1
  private lateinit var accessToken: String
  val shape = GradientDrawable()
  private var localTextPlaceholder: String? = null
  private var imageUrlPlaceholder: String? = null
  private var textPlaceholder: String? = null
  public var myRoom: Room? = null
  public var myActivity: Activity? = null
  public var myPermissionAwareActivity: PermissionAwareActivity? = null
  public var myLocalParticipant: LocalParticipant? = null
  public var savedVolumeControlStream by Delegates.notNull<Int>()
  var localAudioTrack: LocalAudioTrack? = null
  var localVideoTrack: LocalVideoTrack? = null
  private var participantIdentity: String? = null
  lateinit var localVideoView: VideoSink
  var disconnectedFromOnDestroy = false
  private var isSpeakerPhoneEnabled = true
  var mainView: View? = null

  private var mReconnectingProgressBar: ProgressBar? = null
  private var mLocalVideoActionFab: FloatingActionButton? = null
  private var mThumbnailVideoView: VideoView? = null
  private var mPrimaryVideoView: VideoView? = null
  private var mReactApplicationContext: ReactApplicationContext? = null
  private var mThumbnailPlaceHolderView: LinearLayout? = null
  private var mPrimaryPlaceHolderImageView: ImageView? = null
  private var mPlaceHolderView: LinearLayout? = null
  private var mRemotePlacecHolderTextView: AppCompatTextView? = null
  private var mThumbnailPlaceHolderText: AppCompatTextView? = null

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
    mReactApplicationContext = reactApplicationContext
    val inflater = LayoutInflater.from(context)
    mainView = inflater.inflate(R.layout.video_view, this)
    //-------------------------------------------------
    setAccessTokenFromPref()
    mReconnectingProgressBar =
      mainView!!.findViewById<View>(R.id.reconnectingProgressBar) as ProgressBar?
    mThumbnailVideoView = mainView!!.findViewById<View>(R.id.thumbnailVideoView) as VideoView?
    mPrimaryVideoView = mainView!!.findViewById<View>(R.id.primaryVideoView) as VideoView?
    mPrimaryPlaceHolderImageView =
      mainView!!.findViewById<View>(R.id.remotePlaceHolderImageView) as ImageView?
    mThumbnailPlaceHolderView =
      mainView!!.findViewById<View>(R.id.thumbnailPlaceHolderView) as LinearLayout?
    mPlaceHolderView = mainView!!.findViewById<View>(R.id.placeHolderView) as LinearLayout?
    mRemotePlacecHolderTextView =
      mainView!!.findViewById<View>(R.id.remotePlacecHolderTextView) as AppCompatTextView?
    mThumbnailPlaceHolderText =
      mainView!!.findViewById<View>(R.id.thumbnailPlaceHolderText) as AppCompatTextView?
    mThumbnailVideoView!!.visibility = INVISIBLE
    mPrimaryPlaceHolderImageView!!.visibility = INVISIBLE
    mPlaceHolderView!!.visibility = INVISIBLE
    mRemotePlacecHolderTextView!!.visibility = INVISIBLE
    mThumbnailPlaceHolderView!!.visibility = INVISIBLE
    mThumbnailPlaceHolderText!!.visibility = INVISIBLE
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
      // TODO ===== SEND EVENT =============
      audioSwitch.start { audioDevices, audioDevice -> }
    }
    //-------------------------------------------------

    localVideoTrack = if (localVideoTrack == null && checkPermissionForCameraAndMicrophone()) {
      createLocalVideoTrack(
        this.context,
        true,
        cameraCapturerCompat,
        buildVideoFormat()
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
      if(isH264Supported()){
        return  H264Codec()
      }else{
        return Vp8Codec()
      }
    }

  // ===== SETUP =================================================================================
  private fun buildVideoFormat(): VideoFormat {
    return VideoFormat(VideoDimensions.HD_720P_VIDEO_DIMENSIONS, 24)
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

      return EncodingParameters(10000, 16000)
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
      Log.d(TAG, "onReconnected")

      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_RE_CONNECTED, params)
      mReconnectingProgressBar!!.visibility = View.GONE
    }

    override fun onReconnecting(room: Room, twilioException: TwilioException) {
      mReconnectingProgressBar!!.visibility = View.VISIBLE
      Log.d(TAG, "onReconnecting")

    }

    override fun onConnectFailure(room: Room, e: TwilioException) {
      audioSwitch.deactivate()
      val params = Arguments.createMap()
      params.putString(Constants.ERROR, e.toString())
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      sendEvent(reactApplicationContext, Constants.ON_CONNECT_FAILURE, params)
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
        moveLocalVideoToPrimaryView()
      }
    }

    override fun onParticipantConnected(room: Room, participant: RemoteParticipant) {
      Log.d(TAG, "got this al leat")
      addRemoteParticipant(participant)
      Log.d(TAG, "onParticipantConnected")

    }

    override fun onParticipantReconnected(room: Room, participant: RemoteParticipant) {
      Log.d(TAG, "got this al leat then")
      val params = Arguments.createMap()
      params.putString(Constants.ROOM_NAME, room.name)
      params.putString(Constants.ROOM_SID, room.getSid())
      params.putString(Constants.PARTICIPANT_SID, participant.sid)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_RECONNECTED, params)
      Log.d(TAG, "onParticipantReconnected")

      addRemoteParticipant(participant)
    }

    override fun onParticipantDisconnected(room: Room, participant: RemoteParticipant) {
      removeRemoteParticipant(participant)
      Log.d(TAG, "onParticipantDisconnected")

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
        Log.d(TAG, "onAudioTrackPublished")

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
        Log.d(TAG, "onVideoTrackPublished")

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
        Log.d(TAG, "onDataTrackPublished")

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
      setPrimaryViewPlaceholder(true)

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
    //  setPrimaryViewPlaceholder(false)

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
      setPrimaryViewPlaceholder(false)

      Log.i(
        TAG, "onVideoTrackSubscriptionFailed: " +
          "[RemoteParticipant: identity=${remoteParticipant.identity}], " +
          "[RemoteVideoTrackPublication: sid=${remoteVideoTrackPublication.trackSid}, " +
          "name=${remoteVideoTrackPublication.trackName}]" +
          "[TwilioException: code=${twilioException.code}, " +
          "message=${twilioException.message}]"
      )
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
      setPrimaryViewPlaceholder(true)

    }

    override fun onVideoTrackDisabled(
      remoteParticipant: RemoteParticipant,
      remoteVideoTrackPublication: RemoteVideoTrackPublication
    ) {
      val event = buildParticipantVideoEvent(remoteParticipant, remoteVideoTrackPublication)
      sendEvent(reactApplicationContext, Constants.ON_PARTICIPANT_DISABLED_VIDEO_TRACK, event)
      setPrimaryViewPlaceholder(false)
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
      cameraCapturerCompat,
      buildVideoFormat()

    )
  }

  public fun setAccessTokenFromPref() {
    val sharedPref = mReactApplicationContext!!
      .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
    val prefToken = sharedPref.getString(Constants.PREF_TOEKN, null)
    this.accessToken = prefToken.toString();

  }

  fun connectToRoom(src: ReadableMap) {
    // setAccessTokenFromPref()
    if (src.hasKey("token")) {
      this.accessToken = src.getString("token")!!
    }
    if (src.hasKey("imgUriPlaceHolder")) {
      this.imageUrlPlaceholder = src.getString("imgUriPlaceHolder")!!
    }
    if (src.hasKey("textPlaceHolder")) {
      this.textPlaceholder = src.getString("textPlaceHolder")!!
    }
    if (src.hasKey("localTextPlaceHolder")) {
      this.localTextPlaceholder = src.getString("localTextPlaceHolder")!!
    }
    if (this.accessToken == null) return
    if (this.accessToken.isEmpty()) return
    try {
      audioSwitch.activate()
      Log.d(
        TAG, "On connectToRoom =========================\n" +
          " roomName:${src.getString("roomName")!!}\n"
      )

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
        preferAudioCodecs(listOf(G722Codec(),))
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
    } catch (e: Exception) {
      Log.d(TAG, "On connect Exception ${e.toString()}")
    }
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
          audioSwitch.selectDevice(selectedAudioDevice)
        }.create().show()
    }
  }

  fun setPrimaryViewPlaceholder(isVideoAvailable: Boolean) {
    if (isVideoAvailable) {
      mPrimaryVideoView!!.visibility = VISIBLE
      mRemotePlacecHolderTextView!!.visibility = INVISIBLE
      mPrimaryPlaceHolderImageView!!.visibility = INVISIBLE
      mPlaceHolderView!!.visibility = INVISIBLE

    } else {
      if (this.imageUrlPlaceholder != null) {
        Glide.with(context)
          .load(this.imageUrlPlaceholder)
          .into(mPrimaryPlaceHolderImageView!!)
        mPrimaryPlaceHolderImageView!!.visibility = VISIBLE
        mPrimaryVideoView!!.visibility = INVISIBLE

      } else {
        if (this.mRemotePlacecHolderTextView != null) {
          val temp = this.textPlaceholder.toString()
          this.mRemotePlacecHolderTextView!!.text = temp
        } else {
          val temp = "Camera Client closed"
          this.mRemotePlacecHolderTextView!!.text = temp

        }
        mPlaceHolderView!!.visibility = VISIBLE
        mRemotePlacecHolderTextView!!.visibility = VISIBLE
        mPrimaryVideoView!!.visibility = INVISIBLE
      }
    }
  }


  fun setThumbnailViewPlaceholder(isVideoAvailable: Boolean) {
    if (isVideoAvailable) {
      mThumbnailVideoView!!.visibility = VISIBLE
      mThumbnailPlaceHolderView!!.visibility = INVISIBLE
      mThumbnailPlaceHolderText!!.visibility = INVISIBLE
      mThumbnailVideoView!!.elevation= 2F
    } else {

        if (this.mRemotePlacecHolderTextView != null) {
          val temp = this.localTextPlaceholder.toString()
          this.mRemotePlacecHolderTextView!!.text = temp
        } else {
          val temp = "CAMERA CLOSED"
          this.mThumbnailPlaceHolderText!!.text = temp

        }
      mThumbnailPlaceHolderView!!.visibility = VISIBLE
      mThumbnailPlaceHolderText!!.visibility = VISIBLE
        mThumbnailVideoView!!.visibility = INVISIBLE
      }

  }

  /*
   * Called when participant joins the room
   */
  private fun addRemoteParticipant(remoteParticipant: RemoteParticipant) {
    /*
     * This app only displays video for one additional participant per Room
     */

    if (mThumbnailVideoView!!.visibility == View.VISIBLE) {
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


  public fun disconnect() {
    Log.d(TAG, "========= disconnect ======= ")
    val event: WritableMap = WritableNativeMap()
    event.putString(Constants.END_CALL, Constants.END_CALL)
    sendEvent(mReactApplicationContext!!, Constants.END_CALL, event)
    myRoom?.disconnect()

  }

  public fun switchCamera() {

    Log.d(TAG, "========= switchCamera ======= ")
    val cameraSource = cameraCapturerCompat.cameraSource
    cameraCapturerCompat.switchCamera()
    if (mPrimaryVideoView!!.visibility == View.VISIBLE) {
      mPrimaryVideoView!!.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
    } else {
      mPrimaryVideoView!!.mirror = cameraSource == CameraCapturerCompat.Source.BACK_CAMERA
    }
    val event: WritableMap = WritableNativeMap()
    if (cameraSource == CameraCapturerCompat.Source.BACK_CAMERA) {
      event.putString(Constants.ON_CAMERA_SWITCHED, "back")
      sendEvent(mReactApplicationContext!!, Constants.ON_VIDEO_ENABLED, event)
    } else {
      event.putString(Constants.ON_CAMERA_SWITCHED, "front")
      sendEvent(mReactApplicationContext!!, Constants.ON_VIDEO_ENABLED, event)
    }
  }

  public fun enableVideo() {
    Log.d(TAG, "========= disconnect ======= ")
    localVideoTrack?.let {
      val enable = !it.isEnabled
      it.enable(enable)
      val event: WritableMap = WritableNativeMap()
      event.putString(Constants.ON_VIDEO_ENABLED, enable.toString())
      sendEvent(mReactApplicationContext!!, Constants.ON_VIDEO_ENABLED, event)
      setThumbnailViewPlaceholder(enable)
    }
  }

  public fun mute() {
    Log.d(TAG, "========= mute ======= ")
    localAudioTrack?.let {
      val enable = !it.isEnabled
      it.enable(enable)
      val event: WritableMap = WritableNativeMap()
      event.putBoolean(Constants.ON_MUTE, enable)
      sendEvent(mReactApplicationContext!!, Constants.ON_RE_CONNECTED, event)

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
      // TODO ===== SEND EVENT ============= updateAudio
      audioSwitch.start { audioDevices, audioDevice ->/* updateAudioDeviceIcon(audioDevice)*/ }

      if (cameraAndMicPermissionGranted) {
        createAudioAndVideoTracks()
        // TODO ===== SEND EVENT ============= updateAudio
        audioSwitch.start { audioDevices, audioDevice -> }

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
