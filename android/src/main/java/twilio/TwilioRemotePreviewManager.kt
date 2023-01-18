/**
 * Component for Twilio Video participant views.
 *
 *
 * Authors:
 * Jonathan Chang <slycoder></slycoder>@gmail.com>
 */
package com.twilio

import android.util.Log
import com.facebook.react.common.MapBuilder
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.annotations.ReactProp
import tvi.webrtc.RendererCommon
import com.facebook.react.uimanager.ThemedReactContext
import com.twilio.src.Events
import com.twilio.src.NativeView

class TwilioRemotePreviewManager : SimpleViewManager<NativeView>() {
    var myTrackSid: String? = ""
    override fun getName(): String {
        return REACT_CLASS
    }

    @ReactProp(name = "scaleType")
    fun setScaleType(view: NativeView, scaleType: String?) {
        if (scaleType == "fit") {
            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        } else {
            view.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        }
    }

    @ReactProp(name = "trackSid")
    fun setTrackId(view: NativeView, trackSid: String?) {
        Log.i("CustomTwilioVideoView", "Initialize Twilio REMOTE")
        Log.i("CustomTwilioVideoView", trackSid!!)
        myTrackSid = trackSid
      view.registerPrimaryVideoView( trackSid)
    }

    @ReactProp(name = "applyZOrder", defaultBoolean = false)
    fun setApplyZOrder(view: NativeView, applyZOrder: Boolean) {
        view.applyZOrder(applyZOrder)
    }

    override fun createViewInstance(reactContext: ThemedReactContext): NativeView {
      val permissionAwareActivity = reactContext.currentActivity as PermissionAwareActivity?
      return reactContext.currentActivity?.let {
        NativeView(reactContext, true,
          it,permissionAwareActivity!!)
      }!!
    }

    override fun getExportedCustomBubblingEventTypeConstants(): MutableMap<String, Any>? {
        return MapBuilder.builder<String, Any>()
            .put(
              Events.ON_FRAME_DIMENSIONS_CHANGED,
                MapBuilder.of(
                    "phasedRegistrationNames",
                    MapBuilder.of("bubbled",Events.ON_FRAME_DIMENSIONS_CHANGED)
                )
            )
            .build()
    }

    companion object {
        const val REACT_CLASS = "RCTTWRemoteVideoView"
    }
}
