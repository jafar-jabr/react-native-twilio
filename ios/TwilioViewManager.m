#import <React/RCTViewManager.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(TwilioEmitter, RCTEventEmitter)
RCT_EXTERN_METHOD(supportedEvents)
@end
@interface RCT_EXTERN_MODULE(TwilioViewManager, RCTViewManager)
RCT_EXPORT_VIEW_PROPERTY(src, NSDictionary)
RCT_EXTERN_METHOD(switchCamera)
RCT_EXTERN_METHOD(mute)
RCT_EXTERN_METHOD(closeCamera)
RCT_EXTERN_METHOD(endCall)

@end
