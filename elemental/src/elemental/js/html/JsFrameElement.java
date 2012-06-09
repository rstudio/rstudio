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
import elemental.js.dom.JsDocument;
import elemental.svg.SVGDocument;
import elemental.html.FrameElement;
import elemental.js.svg.JsSVGDocument;
import elemental.html.Window;
import elemental.dom.Document;

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

public class JsFrameElement extends JsElement  implements FrameElement {
  protected JsFrameElement() {}

  public final native JsDocument getContentDocument() /*-{
    return this.contentDocument;
  }-*/;

  public final native JsWindow getContentWindow() /*-{
    return this.contentWindow;
  }-*/;

  public final native String getFrameBorder() /*-{
    return this.frameBorder;
  }-*/;

  public final native void setFrameBorder(String param_frameBorder) /*-{
    this.frameBorder = param_frameBorder;
  }-*/;

  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  public final native String getLocation() /*-{
    return this.location;
  }-*/;

  public final native void setLocation(String param_location) /*-{
    this.location = param_location;
  }-*/;

  public final native String getLongDesc() /*-{
    return this.longDesc;
  }-*/;

  public final native void setLongDesc(String param_longDesc) /*-{
    this.longDesc = param_longDesc;
  }-*/;

  public final native String getMarginHeight() /*-{
    return this.marginHeight;
  }-*/;

  public final native void setMarginHeight(String param_marginHeight) /*-{
    this.marginHeight = param_marginHeight;
  }-*/;

  public final native String getMarginWidth() /*-{
    return this.marginWidth;
  }-*/;

  public final native void setMarginWidth(String param_marginWidth) /*-{
    this.marginWidth = param_marginWidth;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native boolean isNoResize() /*-{
    return this.noResize;
  }-*/;

  public final native void setNoResize(boolean param_noResize) /*-{
    this.noResize = param_noResize;
  }-*/;

  public final native String getScrolling() /*-{
    return this.scrolling;
  }-*/;

  public final native void setScrolling(String param_scrolling) /*-{
    this.scrolling = param_scrolling;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  public final native JsSVGDocument getSVGDocument() /*-{
    return this.getSVGDocument();
  }-*/;
}
