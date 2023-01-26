package com.twilio

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.twilio.src.Constants

class TwilioModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {
  private var mReactApplicationContext: ReactApplicationContext? = null

  override fun getName() = "TwilioView"

  init {
    mReactApplicationContext = reactContext
  }
}
