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
import elemental.html.MarqueeElement;
import elemental.js.dom.JsElement;
import elemental.dom.Element;

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

public class JsMarqueeElement extends JsElement  implements MarqueeElement {
  protected JsMarqueeElement() {}

  public final native String getBehavior() /*-{
    return this.behavior;
  }-*/;

  public final native void setBehavior(String param_behavior) /*-{
    this.behavior = param_behavior;
  }-*/;

  public final native String getBgColor() /*-{
    return this.bgColor;
  }-*/;

  public final native void setBgColor(String param_bgColor) /*-{
    this.bgColor = param_bgColor;
  }-*/;

  public final native String getDirection() /*-{
    return this.direction;
  }-*/;

  public final native void setDirection(String param_direction) /*-{
    this.direction = param_direction;
  }-*/;

  public final native String getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(String param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native int getHspace() /*-{
    return this.hspace;
  }-*/;

  public final native void setHspace(int param_hspace) /*-{
    this.hspace = param_hspace;
  }-*/;

  public final native int getLoop() /*-{
    return this.loop;
  }-*/;

  public final native void setLoop(int param_loop) /*-{
    this.loop = param_loop;
  }-*/;

  public final native int getScrollAmount() /*-{
    return this.scrollAmount;
  }-*/;

  public final native void setScrollAmount(int param_scrollAmount) /*-{
    this.scrollAmount = param_scrollAmount;
  }-*/;

  public final native int getScrollDelay() /*-{
    return this.scrollDelay;
  }-*/;

  public final native void setScrollDelay(int param_scrollDelay) /*-{
    this.scrollDelay = param_scrollDelay;
  }-*/;

  public final native boolean isTrueSpeed() /*-{
    return this.trueSpeed;
  }-*/;

  public final native void setTrueSpeed(boolean param_trueSpeed) /*-{
    this.trueSpeed = param_trueSpeed;
  }-*/;

  public final native int getVspace() /*-{
    return this.vspace;
  }-*/;

  public final native void setVspace(int param_vspace) /*-{
    this.vspace = param_vspace;
  }-*/;

  public final native String getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(String param_width) /*-{
    this.width = param_width;
  }-*/;

  public final native void start() /*-{
    this.start();
  }-*/;

  public final native void stop() /*-{
    this.stop();
  }-*/;
}
