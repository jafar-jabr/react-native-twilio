package com.twilio

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.JavaScriptModule
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.*


class TwilioPackage : ReactPackage {
  override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
    return emptyList()
  }

  fun createJSModules(): List<Class<out JavaScriptModule?>?> {
    return emptyList<Class<out JavaScriptModule?>>()
  }
  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {

    return Arrays.asList<ViewManager<*, *>>(
      CustomTwilioVideoViewManager(),
      TwilioRemotePreviewManager(),
      TwilioLocaleViewManager()
    )
  }

}
