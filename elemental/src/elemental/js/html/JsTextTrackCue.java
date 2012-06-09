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
import elemental.html.TextTrack;
import elemental.js.dom.JsDocumentFragment;
import elemental.js.events.JsEvent;
import elemental.dom.DocumentFragment;
import elemental.events.EventListener;
import elemental.html.TextTrackCue;
import elemental.js.events.JsEventListener;
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

public class JsTextTrackCue extends JsElementalMixinBase  implements TextTrackCue {
  protected JsTextTrackCue() {}

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native double getEndTime() /*-{
    return this.endTime;
  }-*/;

  public final native void setEndTime(double param_endTime) /*-{
    this.endTime = param_endTime;
  }-*/;

  public final native String getId() /*-{
    return this.id;
  }-*/;

  public final native void setId(String param_id) /*-{
    this.id = param_id;
  }-*/;

  public final native int getLine() /*-{
    return this.line;
  }-*/;

  public final native void setLine(int param_line) /*-{
    this.line = param_line;
  }-*/;

  public final native EventListener getOnenter() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onenter);
  }-*/;

  public final native void setOnenter(EventListener listener) /*-{
    this.onenter = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native EventListener getOnexit() /*-{
    return @elemental.js.dom.JsElementalMixinBase::getListenerFor(Lcom/google/gwt/core/client/JavaScriptObject;)(this.onexit);
  }-*/;

  public final native void setOnexit(EventListener listener) /*-{
    this.onexit = @elemental.js.dom.JsElementalMixinBase::getHandlerFor(Lelemental/events/EventListener;)(listener);
  }-*/;
  public final native boolean isPauseOnExit() /*-{
    return this.pauseOnExit;
  }-*/;

  public final native void setPauseOnExit(boolean param_pauseOnExit) /*-{
    this.pauseOnExit = param_pauseOnExit;
  }-*/;

  public final native int getPosition() /*-{
    return this.position;
  }-*/;

  public final native void setPosition(int param_position) /*-{
    this.position = param_position;
  }-*/;

  public final native int getSize() /*-{
    return this.size;
  }-*/;

  public final native void setSize(int param_size) /*-{
    this.size = param_size;
  }-*/;

  public final native boolean isSnapToLines() /*-{
    return this.snapToLines;
  }-*/;

  public final native void setSnapToLines(boolean param_snapToLines) /*-{
    this.snapToLines = param_snapToLines;
  }-*/;

  public final native double getStartTime() /*-{
    return this.startTime;
  }-*/;

  public final native void setStartTime(double param_startTime) /*-{
    this.startTime = param_startTime;
  }-*/;

  public final native String getText() /*-{
    return this.text;
  }-*/;

  public final native void setText(String param_text) /*-{
    this.text = param_text;
  }-*/;

  public final native JsTextTrack getTrack() /*-{
    return this.track;
  }-*/;

  public final native String getVertical() /*-{
    return this.vertical;
  }-*/;

  public final native void setVertical(String param_vertical) /*-{
    this.vertical = param_vertical;
  }-*/;

  public final native JsDocumentFragment getCueAsHTML() /*-{
    return this.getCueAsHTML();
  }-*/;
}
