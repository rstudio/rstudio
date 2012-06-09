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
import elemental.html.ImageElement;

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

public class JsImageElement extends JsElement  implements ImageElement {
  protected JsImageElement() {}

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native String getAlt() /*-{
    return this.alt;
  }-*/;

  public final native void setAlt(String param_alt) /*-{
    this.alt = param_alt;
  }-*/;

  public final native String getBorder() /*-{
    return this.border;
  }-*/;

  public final native void setBorder(String param_border) /*-{
    this.border = param_border;
  }-*/;

  public final native boolean isComplete() /*-{
    return this.complete;
  }-*/;

  public final native String getCrossOrigin() /*-{
    return this.crossOrigin;
  }-*/;

  public final native void setCrossOrigin(String param_crossOrigin) /*-{
    this.crossOrigin = param_crossOrigin;
  }-*/;

  public final native int getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(int param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native int getHspace() /*-{
    return this.hspace;
  }-*/;

  public final native void setHspace(int param_hspace) /*-{
    this.hspace = param_hspace;
  }-*/;

  public final native boolean isMap() /*-{
    return this.isMap;
  }-*/;

  public final native void setIsMap(boolean param_isMap) /*-{
    this.isMap = param_isMap;
  }-*/;

  public final native String getLongDesc() /*-{
    return this.longDesc;
  }-*/;

  public final native void setLongDesc(String param_longDesc) /*-{
    this.longDesc = param_longDesc;
  }-*/;

  public final native String getLowsrc() /*-{
    return this.lowsrc;
  }-*/;

  public final native void setLowsrc(String param_lowsrc) /*-{
    this.lowsrc = param_lowsrc;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native int getNaturalHeight() /*-{
    return this.naturalHeight;
  }-*/;

  public final native int getNaturalWidth() /*-{
    return this.naturalWidth;
  }-*/;

  public final native String getSrc() /*-{
    return this.src;
  }-*/;

  public final native void setSrc(String param_src) /*-{
    this.src = param_src;
  }-*/;

  public final native String getUseMap() /*-{
    return this.useMap;
  }-*/;

  public final native void setUseMap(String param_useMap) /*-{
    this.useMap = param_useMap;
  }-*/;

  public final native int getVspace() /*-{
    return this.vspace;
  }-*/;

  public final native void setVspace(int param_vspace) /*-{
    this.vspace = param_vspace;
  }-*/;

  public final native int getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(int param_width) /*-{
    this.width = param_width;
  }-*/;

  public final native int getX() /*-{
    return this.x;
  }-*/;

  public final native int getY() /*-{
    return this.y;
  }-*/;
}
