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
import elemental.html.ValidityState;
import elemental.js.dom.JsElement;
import elemental.dom.Element;
import elemental.js.dom.JsDocument;
import elemental.svg.SVGDocument;
import elemental.html.FormElement;
import elemental.html.ObjectElement;
import elemental.js.svg.JsSVGDocument;
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

public class JsObjectElement extends JsElement  implements ObjectElement {
  protected JsObjectElement() {}

  public final native String getAlign() /*-{
    return this.align;
  }-*/;

  public final native void setAlign(String param_align) /*-{
    this.align = param_align;
  }-*/;

  public final native String getArchive() /*-{
    return this.archive;
  }-*/;

  public final native void setArchive(String param_archive) /*-{
    this.archive = param_archive;
  }-*/;

  public final native String getBorder() /*-{
    return this.border;
  }-*/;

  public final native void setBorder(String param_border) /*-{
    this.border = param_border;
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

  public final native String getCodeType() /*-{
    return this.codeType;
  }-*/;

  public final native void setCodeType(String param_codeType) /*-{
    this.codeType = param_codeType;
  }-*/;

  public final native JsDocument getContentDocument() /*-{
    return this.contentDocument;
  }-*/;

  public final native String getData() /*-{
    return this.data;
  }-*/;

  public final native void setData(String param_data) /*-{
    this.data = param_data;
  }-*/;

  public final native boolean isDeclare() /*-{
    return this.declare;
  }-*/;

  public final native void setDeclare(boolean param_declare) /*-{
    this.declare = param_declare;
  }-*/;

  public final native JsFormElement getForm() /*-{
    return this.form;
  }-*/;

  public final native String getHeight() /*-{
    return this.height;
  }-*/;

  public final native void setHeight(String param_height) /*-{
    this.height = param_height;
  }-*/;

  public final native int getHspace() /*-{
    return this.hspace;
  }-*/;

  public final native void setHspace(int param_hspace) /*-{
    this.hspace = param_hspace;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native String getStandby() /*-{
    return this.standby;
  }-*/;

  public final native void setStandby(String param_standby) /*-{
    this.standby = param_standby;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native void setType(String param_type) /*-{
    this.type = param_type;
  }-*/;

  public final native String getUseMap() /*-{
    return this.useMap;
  }-*/;

  public final native void setUseMap(String param_useMap) /*-{
    this.useMap = param_useMap;
  }-*/;

  public final native String getValidationMessage() /*-{
    return this.validationMessage;
  }-*/;

  public final native JsValidityState getValidity() /*-{
    return this.validity;
  }-*/;

  public final native int getVspace() /*-{
    return this.vspace;
  }-*/;

  public final native void setVspace(int param_vspace) /*-{
    this.vspace = param_vspace;
  }-*/;

  public final native String getWidth() /*-{
    return this.width;
  }-*/;

  public final native void setWidth(String param_width) /*-{
    this.width = param_width;
  }-*/;

  public final native boolean isWillValidate() /*-{
    return this.willValidate;
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native JsSVGDocument getSVGDocument() /*-{
    return this.getSVGDocument();
  }-*/;

  public final native void setCustomValidity(String error) /*-{
    this.setCustomValidity(error);
  }-*/;
}
