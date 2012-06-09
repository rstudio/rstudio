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
package elemental.js.events;
import elemental.html.Window;
import elemental.js.html.JsWindow;
import elemental.events.WheelEvent;
import elemental.events.UIEvent;

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

public class JsWheelEvent extends JsUIEvent  implements WheelEvent {
  protected JsWheelEvent() {}

  public final native boolean isAltKey() /*-{
    return this.altKey;
  }-*/;

  public final native int getClientX() /*-{
    return this.clientX;
  }-*/;

  public final native int getClientY() /*-{
    return this.clientY;
  }-*/;

  public final native boolean isCtrlKey() /*-{
    return this.ctrlKey;
  }-*/;

  public final native boolean isMetaKey() /*-{
    return this.metaKey;
  }-*/;

  public final native int getOffsetX() /*-{
    return this.offsetX;
  }-*/;

  public final native int getOffsetY() /*-{
    return this.offsetY;
  }-*/;

  public final native int getScreenX() /*-{
    return this.screenX;
  }-*/;

  public final native int getScreenY() /*-{
    return this.screenY;
  }-*/;

  public final native boolean isShiftKey() /*-{
    return this.shiftKey;
  }-*/;

  public final native boolean isWebkitDirectionInvertedFromDevice() /*-{
    return this.webkitDirectionInvertedFromDevice;
  }-*/;

  public final native int getWheelDelta() /*-{
    return this.wheelDelta;
  }-*/;

  public final native int getWheelDeltaX() /*-{
    return this.wheelDeltaX;
  }-*/;

  public final native int getWheelDeltaY() /*-{
    return this.wheelDeltaY;
  }-*/;

  public final native int getX() /*-{
    return this.x;
  }-*/;

  public final native int getY() /*-{
    return this.y;
  }-*/;

  public final native void initWebKitWheelEvent(int wheelDeltaX, int wheelDeltaY, Window view, int screenX, int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey) /*-{
    this.initWebKitWheelEvent(wheelDeltaX, wheelDeltaY, view, screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey);
  }-*/;
}
