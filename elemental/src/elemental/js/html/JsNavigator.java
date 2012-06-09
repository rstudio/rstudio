/*
 * Copyright 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package elemental.js.html;
import elemental.js.dom.JsPointerLock;
import elemental.dom.PointerLock;
import elemental.html.NavigatorUserMediaSuccessCallback;
import elemental.html.BatteryManager;
import elemental.js.dom.JsGeolocation;
import elemental.js.util.JsMappable;
import elemental.util.Mappable;
import elemental.html.Navigator;
import elemental.html.DOMMimeTypeArray;
import elemental.dom.Geolocation;
import elemental.html.DOMPluginArray;
import elemental.html.NavigatorUserMediaErrorCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsNavigator extends JsElementalMixinBase  implements Navigator {
  protected JsNavigator() {}

  public final native String getAppCodeName() /*-{
    return this.appCodeName;
  }-*/;

  public final native String getAppName() /*-{
    return this.appName;
  }-*/;

  public final native String getAppVersion() /*-{
    return this.appVersion;
  }-*/;

  public final native boolean isCookieEnabled() /*-{
    return this.cookieEnabled;
  }-*/;

  public final native JsGeolocation getGeolocation() /*-{
    return this.geolocation;
  }-*/;

  public final native String getLanguage() /*-{
    return this.language;
  }-*/;

  public final native JsDOMMimeTypeArray getMimeTypes() /*-{
    return this.mimeTypes;
  }-*/;

  public final native boolean isOnLine() /*-{
    return this.onLine;
  }-*/;

  public final native String getPlatform() /*-{
    return this.platform;
  }-*/;

  public final native JsDOMPluginArray getPlugins() /*-{
    return this.plugins;
  }-*/;

  public final native String getProduct() /*-{
    return this.product;
  }-*/;

  public final native String getProductSub() /*-{
    return this.productSub;
  }-*/;

  public final native String getUserAgent() /*-{
    return this.userAgent;
  }-*/;

  public final native String getVendor() /*-{
    return this.vendor;
  }-*/;

  public final native String getVendorSub() /*-{
    return this.vendorSub;
  }-*/;

  public final native JsBatteryManager getWebkitBattery() /*-{
    return this.webkitBattery;
  }-*/;

  public final native JsPointerLock getWebkitPointer() /*-{
    return this.webkitPointer;
  }-*/;

  public final native void getStorageUpdates() /*-{
    this.getStorageUpdates();
  }-*/;

  public final native boolean javaEnabled() /*-{
    return this.javaEnabled();
  }-*/;

  public final native void registerProtocolHandler(String scheme, String url, String title) /*-{
    this.registerProtocolHandler(scheme, url, title);
  }-*/;

  public final native void webkitGetUserMedia(Mappable options, NavigatorUserMediaSuccessCallback successCallback, NavigatorUserMediaErrorCallback errorCallback) /*-{
    this.webkitGetUserMedia(options, $entry(successCallback.@elemental.html.NavigatorUserMediaSuccessCallback::onNavigatorUserMediaSuccessCallback(Lelemental/dom/LocalMediaStream;)).bind(successCallback), $entry(errorCallback.@elemental.html.NavigatorUserMediaErrorCallback::onNavigatorUserMediaErrorCallback(Lelemental/html/NavigatorUserMediaError;)).bind(errorCallback));
  }-*/;

  public final native void webkitGetUserMedia(Mappable options, NavigatorUserMediaSuccessCallback successCallback) /*-{
    this.webkitGetUserMedia(options, $entry(successCallback.@elemental.html.NavigatorUserMediaSuccessCallback::onNavigatorUserMediaSuccessCallback(Lelemental/dom/LocalMediaStream;)).bind(successCallback));
  }-*/;
}
