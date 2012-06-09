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
import elemental.util.Indexable;
import elemental.js.util.JsIndexable;
import elemental.html.Location;

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

public class JsLocation extends JsElementalMixinBase  implements Location {
  protected JsLocation() {}

  public final native JsIndexable getAncestorOrigins() /*-{
    return this.ancestorOrigins;
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

  public final native String getOrigin() /*-{
    return this.origin;
  }-*/;

  public final native String getPathname() /*-{
    return this.pathname;
  }-*/;

  public final native void setPathname(String param_pathname) /*-{
    this.pathname = param_pathname;
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

  public final native String getSearch() /*-{
    return this.search;
  }-*/;

  public final native void setSearch(String param_search) /*-{
    this.search = param_search;
  }-*/;

  public final native void assign(String url) /*-{
    this.assign(url);
  }-*/;

  public final native void reload() /*-{
    this.reload();
  }-*/;

  public final native void replace(String url) /*-{
    this.replace(url);
  }-*/;
}
