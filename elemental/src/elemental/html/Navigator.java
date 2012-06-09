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
package elemental.html;
import elemental.util.Mappable;
import elemental.dom.Geolocation;
import elemental.dom.PointerLock;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Returns a reference to the navigator object, which can be queried for information about the application running the script.
  */
public interface Navigator {


  /**
    * Returns the internal "code" name of the current browser. Do not rely on this property to return the correct value.
    */
  String getAppCodeName();


  /**
    * Returns the official name of the browser. Do not rely on this property to return the correct value.
    */
  String getAppName();


  /**
    * Returns the version of the browser as a string. Do not rely on this property to return the correct value.
    */
  String getAppVersion();


  /**
    * Returns a boolean indicating whether cookies are enabled in the browser or not.
    */
  boolean isCookieEnabled();

  Geolocation getGeolocation();


  /**
    * Returns a string representing the language version of the browser.
    */
  String getLanguage();


  /**
    * Returns a list of the MIME types supported by the browser.
    */
  DOMMimeTypeArray getMimeTypes();


  /**
    * Returns a boolean indicating whether the browser is working online.
    */
  boolean isOnLine();


  /**
    * Returns a string representing the platform of the browser.
    */
  String getPlatform();


  /**
    * Returns an array of the plugins installed in the browser.
    */
  DOMPluginArray getPlugins();


  /**
    * Returns the product name of the current browser. (e.g. "Gecko")
    */
  String getProduct();


  /**
    * Returns the build number of the current browser (e.g. "20060909")
    */
  String getProductSub();


  /**
    * Returns the user agent string for the current browser.
    */
  String getUserAgent();


  /**
    * Returns the vendor name of the current browser (e.g. "Netscape6")
    */
  String getVendor();


  /**
    * Returns the vendor version number (e.g. "6.1")
    */
  String getVendorSub();


  /**
    * Returns a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/window.navigator.mozBattery">battery</a></code>
 object you can use to get information about the battery charging status.
    */
  BatteryManager getWebkitBattery();


  /**
    * Returns a PointerLock object for the <a title="Mouse Lock API" rel="internal" href="https://developer.mozilla.org/en/API/Mouse_Lock_API">Mouse Lock API</a>.
    */
  PointerLock getWebkitPointer();

  void getStorageUpdates();


  /**
    * Indicates whether the host browser is Java-enabled or not.
    */
  boolean javaEnabled();


  /**
    * Allows web sites to register themselves as a possible handler for a given protocol.
    */
  void registerProtocolHandler(String scheme, String url, String title);

  void webkitGetUserMedia(Mappable options, NavigatorUserMediaSuccessCallback successCallback, NavigatorUserMediaErrorCallback errorCallback);

  void webkitGetUserMedia(Mappable options, NavigatorUserMediaSuccessCallback successCallback);
}
