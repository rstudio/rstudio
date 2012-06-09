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
import elemental.html.TableSectionElement;
import elemental.html.HTMLCollection;

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

public class JsTableSectionElement extends JsElement  implements TableSectionElement {
  protected JsTableSectionElement() {}

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
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

  public final native JsHTMLCollection getRows() /*-{
    return this.rows;
  }-*/;

  public final native String getVAlign() /*-{
    return this.vAlign;
  }-*/;

  public final native void setVAlign(String param_vAlign) /*-{
    this.vAlign = param_vAlign;
  }-*/;

  public final native void deleteRow(int index) /*-{
    this.deleteRow(index);
  }-*/;

  public final native JsElement insertRow(int index) /*-{
    return this.insertRow(index);
  }-*/;
}
