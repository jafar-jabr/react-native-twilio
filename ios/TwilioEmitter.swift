//
//  TwilioEmitter.swift
//  react-native-twilio
//
//  Created by ibrahim on 18/01/2023.
//
import React
import Foundation
@objc(TwilioEmitter)
open class TwilioEmitter: RCTEventEmitter {
    
    public static var ON_FRAME_DIMENSIONS_CHANGED = "onFrameDimensionsChanged"
    public static var ON_CAMERA_SWITCHED = "onCameraSwitched"
    public static var ON_VIDEO_CHANGED = "onVideoChanged"
    public static var ON_AUDIO_CHANGED = "onAudioChanged"
    public static var ROOM_NAME = "roomName"
    public static var ROOM_SID = "roomSid"
    public static var PARTICIPANT_NAME = "participantName"
    public static var PARTICIPANT_SID = "participantSid"
    public static var PARTICIPANT = "participant"
    public static var PARTICIPANTS = "participants"
    public static var LOCAL_PARTICIPANT = "localParticipant"
    public static var TRACK = "track"
    public static var TRACK_SID = "trackSid"
    public static var TRACK_NAME = "trackName"
    public static var ENABLED = "enabled"
    public static var IDENTITY = "identity"
    public static var SID = "sid"
    public static var IS_LOCAL_USER = "isLocalUser"
    public static var QUALITY = "quality"
    public static var END_CALL = "endCall"
    public static var ERROR = "error"
    public static var ON_CONNECTED = "onRoomDidConnect"
    public static var ON_RE_CONNECTED = "onRoomReConnect"
    public static var ON_CONNECT_FAILURE = "onRoomDidFailToConnect"
    public static var ON_DISCONNECTED = "onRoomDidDisconnect"
    public static var ON_PARTICIPANT_CONNECTED = "onRoomParticipantDidConnect"
    public static var ON_PARTICIPANT_RECONNECTED = "onRoomParticipantReconnect"
    public static var ON_PARTICIPANT_DISCONNECTED = "onRoomParticipantDidDisconnect"
    public static var ON_DATATRACK_MESSAGE_RECEIVED = "onDataTrackMessageReceived"
    public static var ON_PARTICIPANT_ADDED_DATA_TRACK = "onParticipantAddedDataTrack"
    public static var ON_PARTICIPANT_REMOVED_DATA_TRACK = "onParticipantRemovedDataTrack"
    public static var ON_PARTICIPANT_ADDED_VIDEO_TRACK = "onParticipantAddedVideoTrack"
    public static var ON_PARTICIPANT_REMOVED_VIDEO_TRACK = "onParticipantRemovedVideoTrack"
    public static var ON_PARTICIPANT_ADDED_AUDIO_TRACK = "onParticipantAddedAudioTrack"
    public static var ON_PARTICIPANT_REMOVED_AUDIO_TRACK = "onParticipantRemovedAudioTrack"
    public static var ON_PARTICIPANT_ENABLED_VIDEO_TRACK = "onParticipantEnabledVideoTrack"
    public static var ON_PARTICIPANT_DISABLED_VIDEO_TRACK = "onParticipantDisabledVideoTrack"
    public static var ON_PARTICIPANT_ENABLED_AUDIO_TRACK = "onParticipantEnabledAudioTrack"
    public static var ON_PARTICIPANT_DISABLED_AUDIO_TRACK = "onParticipantDisabledAudioTrack"
    public static var ON_STATS_RECEIVED = "onStatsReceived"
    public static var ON_NETWORK_QUALITY_LEVELS_CHANGED = "onNetworkQualityLevelsChanged"
    public static var ON_DOMINANT_SPEAKER_CHANGED = "onDominantSpeakerDidChange"
    public static var ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS = "onLocalParticipantSupportedCodecs"
    public static var ON_VIDEO_ENABLED = "onVideoEnabled"
    public static var ON_MUTE = "onAudioEnabled"
    
    var hasListeners = false
    public static var emitter: RCTEventEmitter!

    override init() {
        super.init()
        DispatchQueue.main.async {
            TwilioEmitter.emitter = self
        }
    }

    open  override func supportedEvents() -> [String]! {
        return [
            TwilioEmitter.ON_FRAME_DIMENSIONS_CHANGED,
            TwilioEmitter.ON_CAMERA_SWITCHED,
            TwilioEmitter.ON_VIDEO_CHANGED,
            TwilioEmitter.ON_AUDIO_CHANGED,
            TwilioEmitter.ROOM_NAME,
            "roomSid",
            "participantName",
            "participantSid",
            "participant",
            "participants",
            "localParticipant",
            "track",
            "trackSid",
            "trackName",
            "enabled",
            "identity",
            "sid",
            "isLocalUser",
            "quality",
            "error",
            "endCall",
            "onRoomDidConnect",
            "onRoomReConnect",
            "onRoomDidFailToConnect",
            "onRoomDidDisconnect",
            "onRoomParticipantDidConnect",
            "onRoomParticipantReconnect",
            "onRoomParticipantDidDisconnect",
            "onDataTrackMessageReceived",
            "onParticipantAddedDataTrack",
            "onParticipantRemovedDataTrack",
            "onParticipantAddedVideoTrack",
            "onParticipantRemovedVideoTrack",
            "onParticipantAddedAudioTrack",
            "onParticipantRemovedAudioTrack",
            "onParticipantEnabledVideoTrack",
            "onParticipantDisabledVideoTrack",
            "onParticipantEnabledAudioTrack",
            "onParticipantDisabledAudioTrack",
            "onStatsReceived",
            "onNetworkQualityLevelsChanged",
            "onDominantSpeakerDidChange",
            "onLocalParticipantSupportedCodecs",
            "onVideoEnabled",
            "onAudioEnabled",
        ]
    }

    open  override func startObserving() {
        hasListeners = true
    }

    open  override func stopObserving() {
        hasListeners = false
    }
}
