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
import elemental.js.dom.JsClipboard;
import elemental.dom.Clipboard;
import elemental.events.EventTarget;
import elemental.events.Event;

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

public class JsEvent extends JsElementalMixinBase  implements Event {
  protected JsEvent() {}

  public final native boolean isBubbles() /*-{
    return this.bubbles;
  }-*/;

  public final native boolean isCancelBubble() /*-{
    return this.cancelBubble;
  }-*/;

  public final native void setCancelBubble(boolean param_cancelBubble) /*-{
    this.cancelBubble = param_cancelBubble;
  }-*/;

  public final native boolean isCancelable() /*-{
    return this.cancelable;
  }-*/;

  public final native JsClipboard getClipboardData() /*-{
    return this.clipboardData;
  }-*/;

  public final native EventTarget getCurrentTarget() /*-{
    return this.currentTarget;
  }-*/;

  public final native boolean isDefaultPrevented() /*-{
    return this.defaultPrevented;
  }-*/;

  public final native int getEventPhase() /*-{
    return this.eventPhase;
  }-*/;

  public final native boolean isReturnValue() /*-{
    return this.returnValue;
  }-*/;

  public final native void setReturnValue(boolean param_returnValue) /*-{
    this.returnValue = param_returnValue;
  }-*/;

  public final native EventTarget getSrcElement() /*-{
    return this.srcElement;
  }-*/;

  public final native EventTarget getTarget() /*-{
    return this.target;
  }-*/;

  public final native double getTimeStamp() /*-{
    return this.timeStamp;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg) /*-{
    this.initEvent(eventTypeArg, canBubbleArg, cancelableArg);
  }-*/;

  public final native void preventDefault() /*-{
    this.preventDefault();
  }-*/;

  public final native void stopImmediatePropagation() /*-{
    this.stopImmediatePropagation();
  }-*/;

  public final native void stopPropagation() /*-{
    this.stopPropagation();
  }-*/;
}
