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
package elemental.js.dom;
import elemental.dom.Geolocation;
import elemental.dom.PositionCallback;
import elemental.dom.PositionErrorCallback;

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

public class JsGeolocation extends JsElementalMixinBase  implements Geolocation {
  protected JsGeolocation() {}

  public final native void clearWatch(int watchId) /*-{
    this.clearWatch(watchId);
  }-*/;

  public final native void getCurrentPosition(PositionCallback successCallback) /*-{
    this.getCurrentPosition($entry(successCallback.@elemental.dom.PositionCallback::onPositionCallback(Lelemental/dom/Geoposition;)).bind(successCallback));
  }-*/;

  public final native void getCurrentPosition(PositionCallback successCallback, PositionErrorCallback errorCallback) /*-{
    this.getCurrentPosition($entry(successCallback.@elemental.dom.PositionCallback::onPositionCallback(Lelemental/dom/Geoposition;)).bind(successCallback), $entry(errorCallback.@elemental.dom.PositionErrorCallback::onPositionErrorCallback(Lelemental/dom/PositionError;)).bind(errorCallback));
  }-*/;

  public final native int watchPosition(PositionCallback successCallback) /*-{
    return this.watchPosition($entry(successCallback.@elemental.dom.PositionCallback::onPositionCallback(Lelemental/dom/Geoposition;)).bind(successCallback));
  }-*/;

  public final native int watchPosition(PositionCallback successCallback, PositionErrorCallback errorCallback) /*-{
    return this.watchPosition($entry(successCallback.@elemental.dom.PositionCallback::onPositionCallback(Lelemental/dom/Geoposition;)).bind(successCallback), $entry(errorCallback.@elemental.dom.PositionErrorCallback::onPositionErrorCallback(Lelemental/dom/PositionError;)).bind(errorCallback));
  }-*/;
}
