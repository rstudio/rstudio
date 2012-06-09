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
import elemental.html.HTMLCollection;
import elemental.html.TableCaptionElement;
import elemental.html.TableElement;
import elemental.html.TableSectionElement;

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

public class JsTableElement extends JsElement  implements TableElement {
  protected JsTableElement() {}

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native String getBgColor() /*-{
    return this.bgColor;
  }-*/;

  public final native void setBgColor(String param_bgColor) /*-{
    this.bgColor = param_bgColor;
  }-*/;

  public final native String getBorder() /*-{
    return this.border;
  }-*/;

  public final native void setBorder(String param_border) /*-{
    this.border = param_border;
  }-*/;

  public final native JsTableCaptionElement getCaption() /*-{
    return this.caption;
  }-*/;

  public final native void setCaption(TableCaptionElement param_caption) /*-{
    this.caption = param_caption;
  }-*/;

  public final native String getCellPadding() /*-{
    return this.cellPadding;
  }-*/;

  public final native void setCellPadding(String param_cellPadding) /*-{
    this.cellPadding = param_cellPadding;
  }-*/;

  public final native String getCellSpacing() /*-{
    return this.cellSpacing;
  }-*/;

  public final native void setCellSpacing(String param_cellSpacing) /*-{
    this.cellSpacing = param_cellSpacing;
  }-*/;

  public final native String getFrame() /*-{
    return this.frame;
  }-*/;

  public final native void setFrame(String param_frame) /*-{
    this.frame = param_frame;
  }-*/;

  public final native JsHTMLCollection getRows() /*-{
    return this.rows;
  }-*/;

  public final native String getRules() /*-{
    return this.rules;
  }-*/;

  public final native void setRules(String param_rules) /*-{
    this.rules = param_rules;
  }-*/;

  public final native String getSummary() /*-{
    return this.summary;
  }-*/;

  public final native void setSummary(String param_summary) /*-{
    this.summary = param_summary;
  }-*/;

  public final native JsHTMLCollection getTBodies() /*-{
    return this.tBodies;
  }-*/;

  public final native JsTableSectionElement getTFoot() /*-{
    return this.tFoot;
  }-*/;

  public final native void setTFoot(TableSectionElement param_tFoot) /*-{
    this.tFoot = param_tFoot;
  }-*/;

  public final native JsTableSectionElement getTHead() /*-{
    return this.tHead;
  }-*/;

  public final native void setTHead(TableSectionElement param_tHead) /*-{
    this.tHead = param_tHead;
  }-*/;

  public final native String getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(String param_width) /*-{
    this.width = param_width;
  }-*/;

  public final native JsElement createCaption() /*-{
    return this.createCaption();
  }-*/;

  public final native JsElement createTBody() /*-{
    return this.createTBody();
  }-*/;

  public final native JsElement createTFoot() /*-{
    return this.createTFoot();
  }-*/;

  public final native JsElement createTHead() /*-{
    return this.createTHead();
  }-*/;

  public final native void deleteCaption() /*-{
    this.deleteCaption();
  }-*/;

  public final native void deleteRow(int index) /*-{
    this.deleteRow(index);
  }-*/;

  public final native void deleteTFoot() /*-{
    this.deleteTFoot();
  }-*/;

  public final native void deleteTHead() /*-{
    this.deleteTHead();
  }-*/;

  public final native JsElement insertRow(int index) /*-{
    return this.insertRow(index);
  }-*/;
}
