
import Foundation
import TwilioVideo
import AVFoundation

class TwilioViewController: UIViewController {
    
    
    
    // Video SDK components
    public static var room: Room?
    public static var roomName: String?
    public static var camera: CameraSource?
    public static var localVideoTrack: LocalVideoTrack?
    public static var localAudioTrack: LocalAudioTrack?
    public static var remoteParticipant: RemoteParticipant?

    //
    var remoteView: VideoView?
    
    public static var textLabel = UILabel(frame: CGRect.zero)
    var previewView:VideoView = VideoView.init()
    static var viewRect = CGRectMake(0, 0, 48, 48)
    public static var accessToken : String?
    public static var isStopedCamera: Bool = false
    public static var imgUriPlaceHolder : String?
    public static var imageViewPlaceHolder:UIImageView = UIImageView.init()
    public static var textPlaceHolder : String?
    public static var localTextPlaceHolder : String?
    public static var isCameraClosed : Bool?
    public static var placeHolderContainer: UIView?
    public static var localPlaceHolderContainer: UIView?

    // ------------------------------------------------------------------------------------------------------
    func setDataSrc( data :NSDictionary,rect :CGRect){
        TwilioViewController.viewRect=rect
        self.previewView.frame = rect
        self.previewView.contentMode = .scaleAspectFill;
        self.view.addSubview(self.previewView)
        let dic = NSDictionary(dictionary:data)
        self.startPreview()
        guard let _token = dic.object(forKey: "token") as? String else {
            return
        }
        guard let _roomName = dic.object(forKey: "roomName") as? String else {
            return
        }
        
        guard let _imgUriPlaceHolder = dic.object(forKey: "imgUriPlaceHolder") as? String else {
            return
        }
        guard let _textPlaceHolder = dic.object(forKey: "textPlaceHolder") as? String else {
            return
        }
        guard let _localTextPlaceHolder = dic.object(forKey: "localTextPlaceHolder") as? String else {
            return
        }
        
        TwilioViewController.localTextPlaceHolder = _localTextPlaceHolder
        TwilioViewController.accessToken = _token
        TwilioViewController.roomName = _roomName
        TwilioViewController.imgUriPlaceHolder = _imgUriPlaceHolder
        TwilioViewController.textPlaceHolder = _textPlaceHolder
        print (" ==== imgUriPlaceHolder = \(String(describing: TwilioViewController.imgUriPlaceHolder))")
        print (" ==== textPlaceHolder = \(String(describing: TwilioViewController.textPlaceHolder))")
        
        self.connectToARoom()
    }
    
    // ------------------------------------------------------------------------------------------------------
    func switchCamera() {
        let params =
        [TwilioEmitter.ON_CAMERA_SWITCHED:TwilioEmitter.ON_CAMERA_SWITCHED,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_CAMERA_SWITCHED, body:params);
        
        print (" ==== switchCamera")
        flipCamera()
    }
    
    // ------------------------------------------------------------------------------------------------------
    func mute() {
        
        print (" ==== mute")
        
        if (TwilioViewController.localAudioTrack != nil) {
            TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_MUTE, body: [TwilioEmitter.ON_MUTE:TwilioViewController.localAudioTrack?.isEnabled]);
            
            TwilioViewController.localAudioTrack?.isEnabled = !(TwilioViewController.localAudioTrack?.isEnabled)!
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func closeCamera() {
        
        TwilioViewController.localVideoTrack!.isEnabled = !TwilioViewController.localVideoTrack!.isEnabled;
        if(TwilioViewController.localVideoTrack!.isEnabled){
            TwilioViewController.localPlaceHolderContainer?.isHidden=true
            TwilioViewController.localPlaceHolderContainer!.removeFromSuperview()
            self.view.addSubview(self.previewView)
            
        }else{
            replaceRLocal()
            
        }
        print (" ==== closeCamera")
    }
    
    // ------------------------------------------------------------------------------------------------------
    func endCall() {
        
        if(TwilioViewController.room !== nil){
            TwilioViewController.room!.disconnect()
            print (" ==== endCall")
            TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.END_CALL, body: [TwilioEmitter.END_CALL:TwilioEmitter.END_CALL]);
            
            moveToLocaleVideoView()
        }
    }
    
    /* deinit {
     // We are done with camera
     if let camera = self.camera {
     camera.stopCapture()
     self.camera = nil
     }
     }
     */
    
    // ------------------------------------------------------------------------------------------------------
    //native twilio view ======================================================================
    override func viewDidAppear(_ animated: Bool) {
        print (" ==== viewDidAppear")
    }
    
    // ------------------------------------------------------------------------------------------------------
    override func viewDidLoad() {
        if(TwilioViewController.camera == nil){
            self.startPreview()
        }
        print (" ==== viewDidLoad")
    }
    
    // ------------------------------------------------------------------------------------------------------
    override var prefersHomeIndicatorAutoHidden: Bool {
        print (" ==== prefersHomeIndicatorAutoHidden")
        return TwilioViewController.room != nil
    }
    
    // ------------------------------------------------------------------------------------------------------
    func setupRemoteVideoView() {
        self.remoteView?.isHidden=false
        TwilioViewController.placeHolderContainer?.isHidden=true
        TwilioViewController.imageViewPlaceHolder.isHidden=true
        //self.textLabel.removeFromSuperview()
        self.remoteView = VideoView(frame: CGRect.zero)
        self.remoteView!.tag = 100
        self.view.insertSubview(self.remoteView!, at: 0)
        
        self.remoteView!.contentMode = .scaleAspectFill;
        
        self.remoteView!.frame = self.view.bounds
        
        self.remoteView?.sendSubviewToBack(self.previewView)
        
        self.previewView.frame =
        CGRect(x: self.view.frame.width/3.5, y:self.view.frame.height/25, width: self.view.frame.height/11, height: self.view.frame.height/10)
        self.previewView.layer.cornerRadius = 15.0
    }
    
    // ------------------------------------------------------------------------------------------------------
    func moveToLocaleVideoView() {
        self.remoteView?.removeFromSuperview()
        self.previewView.removeFromSuperview()
        self.previewView = VideoView(frame: CGRect.zero)
        self.view.insertSubview(self.previewView, at: 0)
        self.previewView.frame = self.view.bounds
        self.previewView.contentMode = .scaleAspectFill;
        self.startPreview()
        
    }
    
    // ------------------------------------------------------------------------------------------------------
    func replaceRemote(isImage:Bool){
        self.remoteView?.isHidden=true
        self.remoteView?.removeFromSuperview()
        if(isImage){
            TwilioViewController.imageViewPlaceHolder.isHidden=false
            let image = UIImage(named: "default")
            TwilioViewController.imageViewPlaceHolder.frame=CGRect(x: 0, y:0, width: self.view.frame.width, height: self.view.frame.height)
            TwilioViewController.imageViewPlaceHolder.frame.size=TwilioViewController.imageViewPlaceHolder.intrinsicScaledContentSize!
            
            self.view.addSubview(TwilioViewController.imageViewPlaceHolder)
            TwilioViewController.imageViewPlaceHolder.sendSubviewToBack(self.previewView)
            TwilioViewController.imageViewPlaceHolder.load(url: URL(string:TwilioViewController.imgUriPlaceHolder! )!)
            

        }else{
            TwilioViewController.placeHolderContainer?.isHidden=false
            TwilioViewController.placeHolderContainer = UIView(frame: CGRect.zero)
            TwilioViewController.placeHolderContainer?.frame = CGRect(x: 0, y:0, width: self.view.frame.width, height: self.view.frame.height)
        
            let label:UILabel = UILabel(frame: CGRectMake(0, 0, self.view.frame.width, CGFloat.greatestFiniteMagnitude))
                label.numberOfLines = 0
                label.lineBreakMode = NSLineBreakMode.byWordWrapping
                //label.font = font
                label.text = TwilioViewController.textPlaceHolder
                label.sizeToFit()
            label.center=TwilioViewController.placeHolderContainer!.center
            TwilioViewController.placeHolderContainer!.backgroundColor = UIColor.gray
            self.view.addSubview(TwilioViewController.placeHolderContainer!)
            TwilioViewController.placeHolderContainer!.addSubview(label)
            TwilioViewController.placeHolderContainer!.tag = 200
            self.view.insertSubview(TwilioViewController.placeHolderContainer!, at: 0)
           // self.placeHolderContainer!.frame = self.view.bounds
            TwilioViewController.placeHolderContainer!.sendSubviewToBack(self.previewView)
        
        }
        
    }
    
    // ------------------------------------------------------------------------------------------------------
    func replaceRLocal(){
        self.previewView.isHidden=true
        self.previewView.removeFromSuperview()

        TwilioViewController.localPlaceHolderContainer?.isHidden=false
        TwilioViewController.localPlaceHolderContainer = UIView(frame: CGRect.zero)
        
        let label:UILabel = UILabel(frame: CGRectMake(0, 0, self.view.frame.width, CGFloat.greatestFiniteMagnitude))
            label.numberOfLines = 0
            label.lineBreakMode = NSLineBreakMode.byWordWrapping
            //label.font = font
            label.text = TwilioViewController.localTextPlaceHolder
            label.sizeToFit()
        label.center=TwilioViewController.localPlaceHolderContainer!.center
        TwilioViewController.localPlaceHolderContainer!.backgroundColor = UIColor.gray
        self.view.addSubview(TwilioViewController.localPlaceHolderContainer!)
        TwilioViewController.localPlaceHolderContainer!.addSubview(label)
        TwilioViewController.localPlaceHolderContainer!.tag = 300
        
        TwilioViewController.localPlaceHolderContainer?.frame =
        CGRect(x: self.view.frame.width/3.5, y:self.view.frame.height/25, width: self.view.frame.height/11, height: self.view.frame.height/10)
        TwilioViewController.localPlaceHolderContainer!.layer.cornerRadius = 15.0
        
        self.view.addSubview(TwilioViewController.localPlaceHolderContainer!)
        
        
    }
    // ------------------------------------------------------------------------------------------------------
    func connectToARoom() {
        
        self.prepareLocalMedia()
        let connectOptions = ConnectOptions(token: TwilioViewController.accessToken!) { (builder) in
            
            builder.audioTracks = TwilioViewController.localAudioTrack != nil ? [TwilioViewController.localAudioTrack!] : [LocalAudioTrack]()
            builder.videoTracks = TwilioViewController.localVideoTrack != nil ? [TwilioViewController.localVideoTrack!] : [LocalVideoTrack]()
        }
        
        TwilioViewController.room = TwilioVideoSDK.connect(options: connectOptions, delegate: self)
    }
    
    // ------------------------------------------------------------------------------------------------------
    func connect(sender: AnyObject) {
        self.connectToARoom()
    }
    
    // ------------------------------------------------------------------------------------------------------
    func disconnect(sender: AnyObject) {
        TwilioViewController.room!.disconnect()
        logMessage(messageText: "Attempting to disconnect from room \(TwilioViewController.room!.name)")
    }
    
    // ------------------------------------------------------------------------------------------------------
    func startPreview() {
        let frontCamera = CameraSource.captureDevice(position: .front)
        let backCamera = CameraSource.captureDevice(position: .back)
        
        if (frontCamera != nil || backCamera != nil) {
            
            let options = CameraSourceOptions { (builder) in
                if #available(iOS 13.0, *) {
                    // Track UIWindowScene events for the key window's scene.
                    //  disables multi-window support in the .plist (see UIApplicationSceneManifestKey).
                    builder.orientationTracker = UserInterfaceTracker(scene: UIApplication.shared.keyWindow!.windowScene!)
                }
            }
            // Preview our local camera track in the local video preview view.
            TwilioViewController.camera = CameraSource(options: options, delegate: self)
            TwilioViewController.localVideoTrack = LocalVideoTrack(source: TwilioViewController.camera!, enabled: true, name: "Camera")
            
            // Add renderer to video track for local preview
            TwilioViewController.localVideoTrack!.addRenderer(self.previewView)
            logMessage(messageText: "Video track created")
            
            if (frontCamera != nil && backCamera != nil) {
                // We will flip camera on tap.
                let tap = UITapGestureRecognizer(target: self, action: #selector(TwilioViewController.flipCamera))
                self.previewView.addGestureRecognizer(tap)
            }
            
            TwilioViewController.camera!.startCapture(device: frontCamera != nil ? frontCamera! : backCamera!) { (captureDevice, videoFormat, error) in
                if let error = error {
                    self.logMessage(messageText: "Capture failed with error.\ncode = \((error as NSError).code) error = \(error.localizedDescription)")
                } else {
                    self.previewView.shouldMirror = (captureDevice.position == .front)
                }
            }
        }
        else {
            self.logMessage(messageText:"No front or back capture device found!")
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    @objc func flipCamera() {
        var newDevice: AVCaptureDevice?
        
        if let camera = TwilioViewController.camera, let captureDevice = camera.device {
            if captureDevice.position == .front {
                newDevice = CameraSource.captureDevice(position: .back)
            } else {
                newDevice = CameraSource.captureDevice(position: .front)
            }
            
            if let newDevice = newDevice {
                camera.selectCaptureDevice(newDevice) { (captureDevice, videoFormat, error) in
                    if let error = error {
                        self.logMessage(messageText: "Error selecting capture device.\ncode = \((error as NSError).code) error = \(error.localizedDescription)")
                    } else {
                        self.previewView.shouldMirror = (captureDevice.position == .front)
                    }
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func stopCamera() {
        if  TwilioViewController.isCameraClosed == true{
            if let source = AppScreenSource(), let track = LocalVideoTrack(source: source) {
                TwilioViewController.room?.localParticipant?.unpublishVideoTrack(track)
                print("Stop")
                TwilioViewController.isCameraClosed = false
                TwilioViewController.camera!.stopCapture()
            }
        } else {
            if let source = AppScreenSource(), let track = LocalVideoTrack(source: source) {
                TwilioViewController.room?.localParticipant?.publishVideoTrack(track)
                print("Start")
                TwilioViewController.isCameraClosed = true
                self.startPreview()
            }
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func prepareLocalMedia() {
        
        // We will share local audio and video when we connect to the Room.
        
        // Create an audio track.
        if (TwilioViewController.localAudioTrack == nil) {
            TwilioViewController.localAudioTrack = LocalAudioTrack(options: nil, enabled: true, name: "Microphone")
            
            if (TwilioViewController.localAudioTrack == nil) {
                logMessage(messageText: "Failed to create audio track")
            }
        }
        
        // Create a video track which captures from the camera.
        if (TwilioViewController.localVideoTrack == nil) {
            self.startPreview()
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func renderRemoteParticipant(participant : RemoteParticipant) -> Bool {
        // This example renders the first subscribed RemoteVideoTrack from the RemoteParticipant.
        let videoPublications = participant.remoteVideoTracks
        for publication in videoPublications {
            if let subscribedVideoTrack = publication.remoteTrack,
               publication.isTrackSubscribed {
                setupRemoteVideoView()
                subscribedVideoTrack.addRenderer(self.remoteView!)
                TwilioViewController.remoteParticipant = participant
                return true
            }
        }
        return false
    }
    
    // ------------------------------------------------------------------------------------------------------
    func logMessage(messageText: String) {
        NSLog(messageText)
    }
    
    // ------------------------------------------------------------------------------------------------------
    func renderRemoteParticipants(participants : Array<RemoteParticipant>) {
        for participant in participants {
            // Find the first renderable track.
            if participant.remoteVideoTracks.count > 0,
               renderRemoteParticipant(participant: participant) {
                break
            }
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func cleanupRemoteParticipant() {
        if TwilioViewController.remoteParticipant != nil {
            self.remoteView?.removeFromSuperview()
            self.remoteView = nil
            TwilioViewController.remoteParticipant = nil
        }
    }
    
    // ------------------------------------------------------------------------------------------------------
    func buildParticipant(participant: Participant) -> AnyObject {
        return [TwilioEmitter.IDENTITY:participant.identity,TwilioEmitter.SID:participant.sid] as AnyObject;
    }
    
    // ------------------------------------------------------------------------------------------------------
    func buildTrack(publication: TrackPublication)-> AnyObject {
        let params =
        [TwilioEmitter.TRACK_SID:publication.trackSid,
         TwilioEmitter.TRACK_NAME: publication.trackName,
         TwilioEmitter.ENABLED: publication.isTrackEnabled,
        ] as AnyObject
        
        return params
    }
    
    // ------------------------------------------------------------------------------------------------------
    func buildParticipantTrack(participant: Participant,publication: TrackPublication)-> AnyObject {
        let participantMap = buildParticipant(participant: participant)
        let trackMap = buildTrack(publication: publication)
        
        let params =
        [TwilioEmitter.PARTICIPANT:participantMap,
         TwilioEmitter.TRACK:trackMap,
        ] as AnyObject
        
        return params
    }
}

// **********************************************************************************************************
// MARK:- RoomDelegate
extension TwilioViewController : RoomDelegate {
    func roomDidConnect(room: Room) {
        
        logMessage(messageText: "Connected to room \(room.name) as \(room.localParticipant?.identity ?? "")")
        
        let participants = room.remoteParticipants
        let localParticipant = room.localParticipant
        var participantsArray = [AnyObject]()
        
        for remoteParticipant in room.remoteParticipants {
            remoteParticipant.delegate = self
        }
        for participant in participants {
            participantsArray.append(buildParticipant(participant: participant))
        }
        participantsArray.append(buildParticipant(participant: localParticipant!))
        let params =
        [TwilioEmitter.ROOM_NAME:room.name,
         TwilioEmitter.ROOM_SID:room.sid,
         TwilioEmitter.PARTICIPANTS:participantsArray,
         TwilioEmitter.LOCAL_PARTICIPANT:buildParticipant(participant: localParticipant!),
        ] as [String : Any]
        
        TwilioEmitter.emitter.sendEvent(withName: "onRoomDidConnect", body:params);
    }
    
    func roomDidDisconnect(room: Room, error: Error?) {
        logMessage(messageText: "Disconnected from room \(room.name), error = \(String(describing: error))")
        
        let params =
        [TwilioEmitter.ROOM_NAME:room.name,
         TwilioEmitter.ROOM_SID:room.sid,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_DISCONNECTED, body:params);
        
        self.cleanupRemoteParticipant()
        TwilioViewController.room = nil
        self.moveToLocaleVideoView()
    }
    
    func roomDidFailToConnect(room: Room, error: Error) {
        logMessage(messageText: "Failed to connect to room with error = \(String(describing: error))")
        TwilioViewController.room = nil
        
        let params =
        [
            TwilioEmitter.ERROR:error.localizedDescription,
            TwilioEmitter.ROOM_NAME:room.name,
            TwilioEmitter.ROOM_SID:room.sid,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_CONNECT_FAILURE, body:params);
    }
    
    func roomIsReconnecting(room: Room, error: Error) {
        logMessage(messageText: "Reconnecting to room \(room.name), error = \(String(describing: error))")
        
    }
    
    func roomDidReconnect(room: Room) {
        logMessage(messageText: "Reconnected to room \(room.name)")
        let params =
        [TwilioEmitter.ROOM_NAME:room.name,
         TwilioEmitter.ROOM_SID:room.sid,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_RE_CONNECTED, body:params);
        
    }
    
    func participantDidConnect(room: Room, participant: RemoteParticipant) {
        // Listen for events from all Participants to decide which RemoteVideoTrack to render.
        participant.delegate = self
        let params =
        [TwilioEmitter.ROOM_NAME:room.name,
         TwilioEmitter.ROOM_SID:room.sid,
         TwilioEmitter.PARTICIPANT_SID:participant.sid as Any,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_CONNECTED, body:params);
        
        
        logMessage(messageText: "Participant \(participant.identity) connected with \(participant.remoteAudioTracks.count) audio and \(participant.remoteVideoTracks.count) video tracks")
    }
    
    func participantDidDisconnect(room: Room, participant: RemoteParticipant) {
        logMessage(messageText: "Room \(room.name), Participant \(participant.identity) disconnected")
        
        let params =
        [TwilioEmitter.ROOM_NAME:room.name,
         TwilioEmitter.ROOM_SID:room.sid,
         TwilioEmitter.PARTICIPANT_SID:participant.sid as Any,
        ] as [String : Any]
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_DISCONNECTED, body:params);
        
        self.moveToLocaleVideoView()
    }
}


// **********************************************************************************************************
// MARK:- RemoteParticipantDelegate
extension TwilioViewController : RemoteParticipantDelegate {
    
    func remoteParticipantDidPublishVideoTrack(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {
        // Remote Participant has offered to share the video Track.
        
        logMessage(messageText: "Participant \(participant.identity) published \(publication.trackName) video track")
    }
    
    func remoteParticipantDidUnpublishVideoTrack(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {
        // Remote Participant has stopped sharing the video Track.
        
        logMessage(messageText: "Participant \(participant.identity) unpublished \(publication.trackName) video track")
    }
    
    func remoteParticipantDidPublishAudioTrack(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {
        // Remote Participant has offered to share the audio Track.
        
        logMessage(messageText: "Participant \(participant.identity) published \(publication.trackName) audio track")
    }
    
    func remoteParticipantDidUnpublishAudioTrack(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {
        // Remote Participant has stopped sharing the audio Track.
        
        logMessage(messageText: "Participant \(participant.identity) unpublished \(publication.trackName) audio track")
    }
    
    func didSubscribeToDataTrack(dataTrack: RemoteDataTrack, publication: RemoteDataTrackPublication, participant: RemoteParticipant) {
        
        let params = buildParticipantTrack(participant: participant, publication: publication)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_ADDED_DATA_TRACK, body:params);
        
    }
    
    func didUnsubscribeFromDataTrack(dataTrack: RemoteDataTrack, publication: RemoteDataTrackPublication, participant: RemoteParticipant) {
        
        let params = buildParticipantTrack(participant: participant, publication: publication)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_REMOVED_DATA_TRACK, body:params);
        
    }
    func didSubscribeToVideoTrack(videoTrack: RemoteVideoTrack, publication: RemoteVideoTrackPublication, participant: RemoteParticipant) {
        // The LocalParticipant is subscribed to the RemoteParticipant's video Track. Frames will begin to arrive now.
        let params = buildParticipantTrack(participant: participant, publication: publication)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_ADDED_VIDEO_TRACK, body:params);
        
        
        logMessage(messageText: "Subscribed to \(publication.trackName) video track for Participant \(participant.identity)")
        
        if (TwilioViewController.remoteParticipant == nil) {
            _ = renderRemoteParticipant(participant: participant)
        }
    }
    
    func didUnsubscribeFromVideoTrack(videoTrack: RemoteVideoTrack, publication: RemoteVideoTrackPublication, participant: RemoteParticipant) {
        // We are unsubscribed from the remote Participant's video Track. We will no longer receive the
        // remote Participant's video.
        
        let params = buildParticipantTrack(participant: participant, publication: publication)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_REMOVED_VIDEO_TRACK, body:params);
        
        logMessage(messageText: "Unsubscribed from \(publication.trackName) video track for Participant \(participant.identity)")
        
        if TwilioViewController.remoteParticipant == participant {
            cleanupRemoteParticipant()
            
            // Find another Participant video to render, if possible.
            if var remainingParticipants = TwilioViewController.room?.remoteParticipants,
               let index = remainingParticipants.firstIndex(of: participant) {
                remainingParticipants.remove(at: index)
                renderRemoteParticipants(participants: remainingParticipants)
            }
        }
    }
    
    func didSubscribeToAudioTrack(audioTrack: RemoteAudioTrack, publication: RemoteAudioTrackPublication, participant: RemoteParticipant) {
        // We are subscribed to the remote Participant's audio Track. We will start receiving the
        // remote Participant's audio now.
        let params = buildParticipantTrack(participant: participant, publication: publication)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_ADDED_AUDIO_TRACK, body:params);
        
        logMessage(messageText: "Subscribed to \(publication.trackName) audio track for Participant \(participant.identity)")
    }
    
    func didUnsubscribeFromAudioTrack(audioTrack: RemoteAudioTrack, publication: RemoteAudioTrackPublication, participant: RemoteParticipant) {
        // We are unsubscribed from the remote Participant's audio Track. We will no longer receive the
        // remote Participant's audio.
        let params = buildParticipantTrack(participant: participant, publication: publication)
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_REMOVED_AUDIO_TRACK, body:params);
        
        logMessage(messageText: "Unsubscribed from \(publication.trackName) audio track for Participant \(participant.identity)")
    }
    
    func remoteParticipantDidEnableVideoTrack(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {
        let params = buildParticipantTrack(participant: participant, publication: publication)
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_ENABLED_VIDEO_TRACK, body:params);
        
        self.setupRemoteVideoView()
        logMessage(messageText: "Participant \(participant.identity) enabled \(publication.trackName) video track")
    }
    
    func remoteParticipantDidDisableVideoTrack(participant: RemoteParticipant, publication: RemoteVideoTrackPublication) {
        let params = buildParticipantTrack(participant: participant, publication: publication)
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_DISABLED_VIDEO_TRACK, body:params);
        self.replaceRemote(isImage: TwilioViewController.imgUriPlaceHolder != nil)
        logMessage(messageText: "Participant \(participant.identity) disabled \(publication.trackName) video track")
    }
    
    func remoteParticipantDidEnableAudioTrack(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {
        
        let params = buildParticipantTrack(participant: participant, publication: publication)
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_ENABLED_AUDIO_TRACK, body:params);
        
        logMessage(messageText: "Participant \(participant.identity) enabled \(publication.trackName) audio track")
    }
    
    func remoteParticipantDidDisableAudioTrack(participant: RemoteParticipant, publication: RemoteAudioTrackPublication) {
        
        let params = buildParticipantTrack(participant: participant, publication: publication)
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_PARTICIPANT_DISABLED_AUDIO_TRACK, body:params);
        
        logMessage(messageText: "Participant \(participant.identity) disabled \(publication.trackName) audio track")
    }
    
    func didFailToSubscribeToAudioTrack(publication: RemoteAudioTrackPublication, error: Error, participant: RemoteParticipant) {
        logMessage(messageText: "FailedToSubscribe \(publication.trackName) audio track, error = \(String(describing: error))")
    }
    
    func didFailToSubscribeToVideoTrack(publication: RemoteVideoTrackPublication, error: Error, participant: RemoteParticipant) {
        logMessage(messageText: "FailedToSubscribe \(publication.trackName) video track, error = \(String(describing: error))")
    }
    
    func remoteParticipantNetworkQualityLevelDidChange(participant: RemoteParticipant, networkQualityLevel: NetworkQualityLevel) {
        let params = buildParticipant(participant: participant)
        let params2 =
        [TwilioEmitter.IS_LOCAL_USER:true,
         TwilioEmitter.QUALITY:networkQualityLevel.rawValue - 1,
        ] as [String : Any]
        params.add(params2)
        
        TwilioEmitter.emitter.sendEvent(withName: TwilioEmitter.ON_NETWORK_QUALITY_LEVELS_CHANGED, body:params);
        
    }
}

// **********************************************************************************************************
// MARK:- CameraSourceDelegate
extension TwilioViewController : CameraSourceDelegate {
    func cameraSourceDidFail(source: CameraSource, error: Error) {
        logMessage(messageText: "Camera source failed with error: \(error.localizedDescription)")
    }
}

// **********************************************************************************************************
// MARK:- CameraCapturerDelegate
/*extension TwilioViewController : TVICameraCapturerDelegate {
 func cameraCapturer(_ capturer: TVICameraCapturer, didStartWith source: TVICameraCaptureSource) {
 // Layout the camera preview with dimensions appropriate for our orientation.
 self.view.setNeedsLayout()
 
 if (TwilioViewController.localVideoTrack!.isEnabled) {
 TwilioViewController.localVideoTrack!.isEnabled = true;
 }
 }
 
 func cameraCapturerWasInterrupted(_ capturer: CameraCapturer, reason: AVCaptureSessionInterruptionReason) {
 TwilioViewController.localVideoTrack!.isEnabled = false
 }
 }
 */


extension UIImageView {
    func load(url: URL) {
        DispatchQueue.global().async { [weak self] in
            if let data = try? Data(contentsOf: url) {
                if let image = UIImage(data: data) {
                    DispatchQueue.main.async {
                        self?.image = image
                    }
                }
            }
        }
    }
    var intrinsicScaledContentSize: CGSize? {
        switch contentMode {
        case .scaleAspectFit:
            // aspect fit
            if let image = self.image {
                let imageWidth = image.size.width
                let imageHeight = image.size.height
                let viewWidth = self.frame.size.width
                
                let ratio = viewWidth/imageWidth
                let scaledHeight = imageHeight * ratio
                
                return CGSize(width: viewWidth, height: scaledHeight)
            }
        case .scaleAspectFill:
            // aspect fill
            if let image = self.image {
                let imageWidth = image.size.width
                let imageHeight = image.size.height
                let viewHeight = self.frame.size.width
                
                let ratio = viewHeight/imageHeight
                let scaledWidth = imageWidth * ratio
                
                return CGSize(width: scaledWidth, height: imageHeight)
            }
            
        default: return self.bounds.size
        }
        return nil

    }
}
