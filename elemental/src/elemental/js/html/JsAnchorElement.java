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
import elemental.html.AnchorElement;
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

public class JsAnchorElement extends JsElement  implements AnchorElement {
  protected JsAnchorElement() {}

  public final native String getCharset() /*-{
    return this.charset;
  }-*/;

  public final native void setCharset(String param_charset) /*-{
    this.charset = param_charset;
  }-*/;

  public final native String getCoords() /*-{
    return this.coords;
  }-*/;

  public final native void setCoords(String param_coords) /*-{
    this.coords = param_coords;
  }-*/;

  public final native String getDownload() /*-{
    return this.download;
  }-*/;

  public final native void setDownload(String param_download) /*-{
    this.download = param_download;
  }-*/;

  public final native String getHash() /*-{
    return this.hash;
  }-*/;

  public final native void setHash(String param_hash) /*-{
    this.hash = param_hash;
  }-*/;

  public final native String getHost() /*-{
    return this.host;
  }-*/;

  public final native void setHost(String param_host) /*-{
    this.host = param_host;
  }-*/;

  public final native String getHostname() /*-{
    return this.hostname;
  }-*/;

  public final native void setHostname(String param_hostname) /*-{
    this.hostname = param_hostname;
  }-*/;

  public final native String getHref() /*-{
    return this.href;
  }-*/;

  public final native void setHref(String param_href) /*-{
    this.href = param_href;
  }-*/;

  public final native String getHreflang() /*-{
    return this.hreflang;
  }-*/;

  public final native void setHreflang(String param_hreflang) /*-{
    this.hreflang = param_hreflang;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native String getOrigin() /*-{
    return this.origin;
  }-*/;

  public final native String getPathname() /*-{
    return this.pathname;
  }-*/;

  public final native void setPathname(String param_pathname) /*-{
    this.pathname = param_pathname;
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

  public final native void setPort(String param_port) /*-{
    this.port = param_port;
  }-*/;

  public final native String getProtocol() /*-{
    return this.protocol;
  }-*/;

  public final native void setProtocol(String param_protocol) /*-{
    this.protocol = param_protocol;
  }-*/;

  public final native String getRel() /*-{
    return this.rel;
  }-*/;

  public final native void setRel(String param_rel) /*-{
    this.rel = param_rel;
  }-*/;

  public final native String getRev() /*-{
    return this.rev;
  }-*/;

  public final native void setRev(String param_rev) /*-{
    this.rev = param_rev;
  }-*/;

  public final native String getSearch() /*-{
    return this.search;
  }-*/;

  public final native void setSearch(String param_search) /*-{
    this.search = param_search;
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

  public final native String getText() /*-{
    return this.text;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;
}
