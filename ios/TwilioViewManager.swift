import UIKit
import Foundation
import React
import AVFoundation
import TwilioVideo
import AVFoundation

@objc(TwilioViewManager)
class TwilioViewManager: RCTViewManager {
    
    override func view() -> (TwilioView) {
        return TwilioView()
    }
    
    @objc override static func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    func methodQueue() -> DispatchQueue {
        return bridge.uiManager.methodQueue
    }
    
    @objc
    func switchCamera() {
        DispatchQueue.main.async {
            self.view().switchCamera()
        }
    }
    
    @objc
    func mute() {
        DispatchQueue.main.async {
            self.view().mute()
        }
    }
    
    @objc
    func closeCamera() {
        DispatchQueue.main.async {
            self.view().closeCamera()
        }
    }
    
    @objc
    func endCall() {
        DispatchQueue.main.async {
            self.view().endCall()
        }
    }
}

// **********************************************************************************************************
class TwilioView : UIView {
    var _rootController = TwilioViewController.init();
    var _src = NSDictionary()
    var _rect = CGRectMake(0, 0, 100, 100)
    var _previewView:VideoView = VideoView.init()
    var _camera: CameraSource = CameraSource.init()
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(_rootController.view);
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc var initialize: NSDictionary = [:]  {
        didSet {
            _src=src
        }
    }
    
    @objc var src: NSDictionary = [:]  {
        didSet {
            _src=src
        }
    }
    
    public func switchCamera() {
        _rootController.switchCamera()
    }
    
    
    func mute() {
        _rootController.mute()
    }
    
    
    func closeCamera() {
        _rootController.closeCamera()
    }
    
    func endCall() {
        _rootController.endCall()
    }
    
    
    func videoView(_ videoView: VideoView, didChangeVideoSize size: CGSize) {
        if (self._previewView == videoView) {
            //self.videoSize = size
        }
        self.setNeedsLayout()
    }
    
    override func layoutSubviews() {
        self._rect = CGRect(x: 0, y:0, width: frame.width, height: frame.height)
        self._rootController.setDataSrc(data: _src,rect: _rect)
    }
    
}
