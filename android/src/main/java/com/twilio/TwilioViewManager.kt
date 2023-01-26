package com.twilio

import android.app.Activity
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.twilio.src.Constants
import com.twilio.src.NativeView

class TwilioViewManager (reactApplicationContext: ReactApplicationContext) : SimpleViewManager<NativeView>() {
  override fun getName() = "TwilioView"
  private var mReactApplicationContext: ReactApplicationContext? = null
  private var nativeView: NativeView? = null

  init {
    mReactApplicationContext=reactApplicationContext
  }
  override fun createViewInstance(reactContext: ThemedReactContext): NativeView {
    val permissionAwareActivity = reactContext.currentActivity as PermissionAwareActivity?
    return NativeView(reactContext, mReactApplicationContext!!,true,reactContext.currentActivity!!,permissionAwareActivity!!)

  }

  @ReactProp(name = "src")
  fun setSrc(view: NativeView, src: ReadableMap?) {
    if(view!=null&&src!=null){
      if(src.hasKey("roomName"))
        view.connectToRoom(src)
    }
  }

  override fun receiveCommand(root: NativeView, commandId: String?, args: ReadableArray?) {
    Log.d("NativeViewTwilio", "receiveCommand commandId ${commandId}")
    when(commandId){
      "switchCamera" -> root.switchCamera();
      "mute" -> root.mute();
      "closeCamera" -> root.enableVideo();
      "endCall" -> root.disconnect();
      else -> {
        println("default");
      }
    }
    //super.receiveCommand(root, commandId, args)
  }
}
