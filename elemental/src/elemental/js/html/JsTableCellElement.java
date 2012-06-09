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
import elemental.html.TableCellElement;

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

public class JsTableCellElement extends JsElement  implements TableCellElement {
  protected JsTableCellElement() {}

  public final native String getAbbr() /*-{
    return this.abbr;
  }-*/;

  public final native void setAbbr(String param_abbr) /*-{
    this.abbr = param_abbr;
  }-*/;

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native String getAxis() /*-{
    return this.axis;
  }-*/;

  public final native void setAxis(String param_axis) /*-{
    this.axis = param_axis;
  }-*/;

  public final native String getBgColor() /*-{
    return this.bgColor;
  }-*/;

  public final native void setBgColor(String param_bgColor) /*-{
    this.bgColor = param_bgColor;
  }-*/;

  public final native int getCellIndex() /*-{
    return this.cellIndex;
  }-*/;

  public final native String getCh() /*-{
    return this.ch;
  }-*/;

  public final native void setCh(String param_ch) /*-{
    this.ch = param_ch;
  }-*/;

  public final native String getChOff() /*-{
    return this.chOff;
  }-*/;

  public final native void setChOff(String param_chOff) /*-{
    this.chOff = param_chOff;
  }-*/;

  public final native int getColSpan() /*-{
    return this.colSpan;
  }-*/;

  public final native void setColSpan(int param_colSpan) /*-{
    this.colSpan = param_colSpan;
  }-*/;

  public final native String getHeaders() /*-{
    return this.headers;
  }-*/;

  public final native void setHeaders(String param_headers) /*-{
    this.headers = param_headers;
  }-*/;

  public final native String getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(String param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native boolean isNoWrap() /*-{
    return this.noWrap;
  }-*/;

  public final native void setNoWrap(boolean param_noWrap) /*-{
    this.noWrap = param_noWrap;
  }-*/;

  public final native int getRowSpan() /*-{
    return this.rowSpan;
  }-*/;

  public final native void setRowSpan(int param_rowSpan) /*-{
    this.rowSpan = param_rowSpan;
  }-*/;

  public final native String getScope() /*-{
    return this.scope;
  }-*/;

  public final native void setScope(String param_scope) /*-{
    this.scope = param_scope;
  }-*/;

  public final native String getVAlign() /*-{
    return this.vAlign;
  }-*/;

  public final native void setVAlign(String param_vAlign) /*-{
    this.vAlign = param_vAlign;
  }-*/;

  public final native String getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(String param_width) /*-{
    this.width = param_width;
  }-*/;
}
