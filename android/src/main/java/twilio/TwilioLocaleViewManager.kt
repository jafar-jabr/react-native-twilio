package com.twilio


import android.view.View
import com.facebook.react.common.MapBuilder
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.twilio.src.Events
import com.twilio.src.NativeView
import tvi.webrtc.RendererCommon

class TwilioLocaleViewManager : SimpleViewManager<NativeView>() {

  override fun createViewInstance(reactContext: ThemedReactContext): NativeView {
    val permissionAwareActivity = reactContext.currentActivity as PermissionAwareActivity?
    return reactContext.currentActivity?.let {
      NativeView(reactContext, false,
        it,permissionAwareActivity!!)
    }!!

  }

  val REACT_CLASS = "RCTTWLocalVideoView"

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

  @ReactProp(name = "applyZOrder", defaultBoolean = true)
  fun setApplyZOrder(view: NativeView, applyZOrder: Boolean) {
    view.applyZOrder(applyZOrder)
  }

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any>? {
    return MapBuilder.of(
      Events.ON_FRAME_DIMENSIONS_CHANGED,
      MapBuilder.of("registrationName", Events.ON_FRAME_DIMENSIONS_CHANGED)
    )
  }

}
