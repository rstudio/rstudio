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
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.html.AreaElement;

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

public class JsAreaElement extends JsElement  implements AreaElement {
  protected JsAreaElement() {}

  public final native String getAlt() /*-{
    return this.alt;
  }-*/;

  public final native void setAlt(String param_alt) /*-{
    this.alt = param_alt;
  }-*/;

  public final native String getCoords() /*-{
    return this.coords;
  }-*/;

  public final native void setCoords(String param_coords) /*-{
    this.coords = param_coords;
  }-*/;

  public final native String getHash() /*-{
    return this.hash;
  }-*/;

  public final native String getHost() /*-{
    return this.host;
  }-*/;

  public final native String getHostname() /*-{
    return this.hostname;
  }-*/;

  public final native String getHref() /*-{
    return this.href;
  }-*/;

  public final native void setHref(String param_href) /*-{
    this.href = param_href;
  }-*/;

  public final native boolean isNoHref() /*-{
    return this.noHref;
  }-*/;

  public final native void setNoHref(boolean param_noHref) /*-{
    this.noHref = param_noHref;
  }-*/;

  public final native String getPathname() /*-{
    return this.pathname;
  }-*/;

  public final native String getPing() /*-{
    return this.ping;
  }-*/;

  public final native void setPing(String param_ping) /*-{
    this.ping = param_ping;
  }-*/;

  public final native String getPort() /*-{
    return this.port;
  }-*/;

  public final native String getProtocol() /*-{
    return this.protocol;
  }-*/;

  public final native String getSearch() /*-{
    return this.search;
  }-*/;

  public final native String getShape() /*-{
    return this.shape;
  }-*/;

  public final native void setShape(String param_shape) /*-{
    this.shape = param_shape;
  }-*/;

  public final native String getTarget() /*-{
    return this.target;
  }-*/;

  public final native void setTarget(String param_target) /*-{
    this.target = param_target;
  }-*/;
}
