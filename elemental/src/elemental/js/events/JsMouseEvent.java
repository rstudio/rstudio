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
import elemental.dom.Node;
import elemental.js.html.JsWindow;
import elemental.events.EventTarget;
import elemental.events.MouseEvent;
import elemental.js.dom.JsNode;
import elemental.js.dom.JsClipboard;
import elemental.dom.Clipboard;
import elemental.html.Window;
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

public class JsMouseEvent extends JsUIEvent  implements MouseEvent {
  protected JsMouseEvent() {}

  public final native boolean isAltKey() /*-{
    return this.altKey;
  }-*/;

  public final native int getButton() /*-{
    return this.button;
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

  public final native JsClipboard getDataTransfer() /*-{
    return this.dataTransfer;
  }-*/;

  public final native JsNode getFromElement() /*-{
    return this.fromElement;
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

  public final native EventTarget getRelatedTarget() /*-{
    return this.relatedTarget;
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

  public final native JsNode getToElement() /*-{
    return this.toElement;
  }-*/;

  public final native int getWebkitMovementX() /*-{
    return this.webkitMovementX;
  }-*/;

  public final native int getWebkitMovementY() /*-{
    return this.webkitMovementY;
  }-*/;

  public final native int getX() /*-{
    return this.x;
  }-*/;

  public final native int getY() /*-{
    return this.y;
  }-*/;

  public final native void initMouseEvent(String type, boolean canBubble, boolean cancelable, Window view, int detail, int screenX, int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey, int button, EventTarget relatedTarget) /*-{
    this.initMouseEvent(type, canBubble, cancelable, view, detail, screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button, relatedTarget);
  }-*/;
}
