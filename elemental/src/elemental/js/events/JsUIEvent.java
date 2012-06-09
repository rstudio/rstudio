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
import elemental.events.Event;
import elemental.js.html.JsWindow;
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

public class JsUIEvent extends JsEvent  implements UIEvent {
  protected JsUIEvent() {}

  public final native int getCharCode() /*-{
    return this.charCode;
  }-*/;

  public final native int getDetail() /*-{
    return this.detail;
  }-*/;

  public final native int getKeyCode() /*-{
    return this.keyCode;
  }-*/;

  public final native int getLayerX() /*-{
    return this.layerX;
  }-*/;

  public final native int getLayerY() /*-{
    return this.layerY;
  }-*/;

  public final native int getPageX() /*-{
    return this.pageX;
  }-*/;

  public final native int getPageY() /*-{
    return this.pageY;
  }-*/;

  public final native JsWindow getView() /*-{
    return this.view;
  }-*/;

  public final native int getWhich() /*-{
    return this.which;
  }-*/;

  public final native void initUIEvent(String type, boolean canBubble, boolean cancelable, Window view, int detail) /*-{
    this.initUIEvent(type, canBubble, cancelable, view, detail);
  }-*/;
}
