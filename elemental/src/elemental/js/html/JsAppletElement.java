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
import elemental.html.AppletElement;

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

public class JsAppletElement extends JsElement  implements AppletElement {
  protected JsAppletElement() {}

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

  public final native String getArchive() /*-{
    return this.archive;
  }-*/;

  public final native void setArchive(String param_archive) /*-{
    this.archive = param_archive;
  }-*/;

  public final native String getCode() /*-{
    return this.code;
  }-*/;

  public final native void setCode(String param_code) /*-{
    this.code = param_code;
  }-*/;

  public final native String getCodeBase() /*-{
    return this.codeBase;
  }-*/;

  public final native void setCodeBase(String param_codeBase) /*-{
    this.codeBase = param_codeBase;
  }-*/;

  public final native String getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(String param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native String getHspace() /*-{
    return this.hspace;
  }-*/;

  public final native void setHspace(String param_hspace) /*-{
    this.hspace = param_hspace;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native String getObject() /*-{
    return this.object;
  }-*/;

  public final native void setObject(String param_object) /*-{
    this.object = param_object;
  }-*/;

  public final native String getVspace() /*-{
    return this.vspace;
  }-*/;

  public final native void setVspace(String param_vspace) /*-{
    this.vspace = param_vspace;
  }-*/;

  public final native String getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(String param_width) /*-{
    this.width = param_width;
  }-*/;
}
