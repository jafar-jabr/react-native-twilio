package com.twilio.src

class Events {
  companion object {
    var ON_FRAME_DIMENSIONS_CHANGED = "onFrameDimensionsChanged"
    var ON_CAMERA_SWITCHED = "onCameraSwitched"
    var ON_VIDEO_CHANGED = "onVideoChanged"
    var ON_AUDIO_CHANGED = "onAudioChanged"
    var ON_CONNECTED = "onRoomDidConnect"
    var ON_CONNECT_FAILURE = "onRoomDidFailToConnect"
    var ON_DISCONNECTED = "onRoomDidDisconnect"
    var ON_PARTICIPANT_CONNECTED = "onRoomParticipantDidConnect"
    var ON_PARTICIPANT_DISCONNECTED = "onRoomParticipantDidDisconnect"
    var ON_DATATRACK_MESSAGE_RECEIVED = "onDataTrackMessageReceived"
    var ON_PARTICIPANT_ADDED_DATA_TRACK = "onParticipantAddedDataTrack"
    var ON_PARTICIPANT_REMOVED_DATA_TRACK = "onParticipantRemovedDataTrack"
    var ON_PARTICIPANT_ADDED_VIDEO_TRACK = "onParticipantAddedVideoTrack"
    var ON_PARTICIPANT_REMOVED_VIDEO_TRACK = "onParticipantRemovedVideoTrack"
    var ON_PARTICIPANT_ADDED_AUDIO_TRACK = "onParticipantAddedAudioTrack"
    var ON_PARTICIPANT_REMOVED_AUDIO_TRACK = "onParticipantRemovedAudioTrack"
    var ON_PARTICIPANT_ENABLED_VIDEO_TRACK = "onParticipantEnabledVideoTrack"
    var ON_PARTICIPANT_DISABLED_VIDEO_TRACK = "onParticipantDisabledVideoTrack"
    var ON_PARTICIPANT_ENABLED_AUDIO_TRACK = "onParticipantEnabledAudioTrack"
    var ON_PARTICIPANT_DISABLED_AUDIO_TRACK = "onParticipantDisabledAudioTrack"
    var ON_STATS_RECEIVED = "onStatsReceived"
    var ON_NETWORK_QUALITY_LEVELS_CHANGED = "onNetworkQualityLevelsChanged"
    var ON_DOMINANT_SPEAKER_CHANGED = "onDominantSpeakerDidChange"
    var ON_LOCAL_PARTICIPANT_SUPPORTED_CODECS = "onLocalParticipantSupportedCodecs"
  }
}
