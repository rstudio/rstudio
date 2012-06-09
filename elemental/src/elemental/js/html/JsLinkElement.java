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
import elemental.dom.DOMSettableTokenList;
import elemental.js.dom.JsDOMSettableTokenList;
import elemental.stylesheets.StyleSheet;
import elemental.js.stylesheets.JsStyleSheet;
import elemental.html.LinkElement;

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

public class JsLinkElement extends JsElement  implements LinkElement {
  protected JsLinkElement() {}

  public final native String getCharset() /*-{
    return this.charset;
  }-*/;

  public final native void setCharset(String param_charset) /*-{
    this.charset = param_charset;
  }-*/;

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
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

  public final native String getMedia() /*-{
    return this.media;
  }-*/;

  public final native void setMedia(String param_media) /*-{
    this.media = param_media;
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

  public final native JsStyleSheet getSheet() /*-{
    return this.sheet;
  }-*/;

  public final native JsDOMSettableTokenList getSizes() /*-{
    return this.sizes;
  }-*/;

  public final native void setSizes(DOMSettableTokenList param_sizes) /*-{
    this.sizes = param_sizes;
  }-*/;

  public final native String getTarget() /*-{
    return this.target;
  }-*/;

  public final native void setTarget(String param_target) /*-{
    this.target = param_target;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;
}
