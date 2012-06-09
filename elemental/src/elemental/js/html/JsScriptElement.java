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
import elemental.html.ScriptElement;

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

public class JsScriptElement extends JsElement  implements ScriptElement {
  protected JsScriptElement() {}

  public final native boolean isAsync() /*-{
    return this.async;
  }-*/;

  public final native void setAsync(boolean param_async) /*-{
    this.async = param_async;
  }-*/;

  public final native String getCharset() /*-{
    return this.charset;
  }-*/;

  public final native void setCharset(String param_charset) /*-{
    this.charset = param_charset;
  }-*/;

  public final native String getCrossOrigin() /*-{
    return this.crossOrigin;
  }-*/;

  public final native void setCrossOrigin(String param_crossOrigin) /*-{
    this.crossOrigin = param_crossOrigin;
  }-*/;

  public final native boolean isDefer() /*-{
    return this.defer;
  }-*/;

  public final native void setDefer(boolean param_defer) /*-{
    this.defer = param_defer;
  }-*/;

  public final native String getEvent() /*-{
    return this.event;
  }-*/;

  public final native void setEvent(String param_event) /*-{
    this.event = param_event;
  }-*/;

  public final native String getHtmlFor() /*-{
    return this.htmlFor;
  }-*/;

  public final native void setHtmlFor(String param_htmlFor) /*-{
    this.htmlFor = param_htmlFor;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native String getText() /*-{
    return this.text;
  }-*/;

  public final native void setText(String param_text) /*-{
    this.text = param_text;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;
}
