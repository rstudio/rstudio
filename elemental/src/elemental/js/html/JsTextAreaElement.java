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
import elemental.html.TextAreaElement;
import elemental.dom.Element;
import elemental.html.FormElement;
import elemental.js.dom.JsNodeList;
import elemental.dom.NodeList;

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

public class JsTextAreaElement extends JsElement  implements TextAreaElement {
  protected JsTextAreaElement() {}

  public final native boolean isAutofocus() /*-{
    return this.autofocus;
  }-*/;

  public final native void setAutofocus(boolean param_autofocus) /*-{
    this.autofocus = param_autofocus;
  }-*/;

  public final native int getCols() /*-{
    return this.cols;
  }-*/;

  public final native void setCols(int param_cols) /*-{
    this.cols = param_cols;
  }-*/;

  public final native String getDefaultValue() /*-{
    return this.defaultValue;
  }-*/;

  public final native void setDefaultValue(String param_defaultValue) /*-{
    this.defaultValue = param_defaultValue;
  }-*/;

  public final native String getDirName() /*-{
    return this.dirName;
  }-*/;

  public final native void setDirName(String param_dirName) /*-{
    this.dirName = param_dirName;
  }-*/;

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
  }-*/;

  public final native JsFormElement getForm() /*-{
    return this.form;
  }-*/;

  public final native JsNodeList getLabels() /*-{
    return this.labels;
  }-*/;

  public final native int getMaxLength() /*-{
    return this.maxLength;
  }-*/;

  public final native void setMaxLength(int param_maxLength) /*-{
    this.maxLength = param_maxLength;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native String getPlaceholder() /*-{
    return this.placeholder;
  }-*/;

  public final native void setPlaceholder(String param_placeholder) /*-{
    this.placeholder = param_placeholder;
  }-*/;

  public final native boolean isReadOnly() /*-{
    return this.readOnly;
  }-*/;

  public final native void setReadOnly(boolean param_readOnly) /*-{
    this.readOnly = param_readOnly;
  }-*/;

  public final native boolean isRequired() /*-{
    return this.required;
  }-*/;

  public final native void setRequired(boolean param_required) /*-{
    this.required = param_required;
  }-*/;

  public final native int getRows() /*-{
    return this.rows;
  }-*/;

  public final native void setRows(int param_rows) /*-{
    this.rows = param_rows;
  }-*/;

  public final native String getSelectionDirection() /*-{
    return this.selectionDirection;
  }-*/;

  public final native void setSelectionDirection(String param_selectionDirection) /*-{
    this.selectionDirection = param_selectionDirection;
  }-*/;

  public final native int getSelectionEnd() /*-{
    return this.selectionEnd;
  }-*/;

  public final native void setSelectionEnd(int param_selectionEnd) /*-{
    this.selectionEnd = param_selectionEnd;
  }-*/;

  public final native int getSelectionStart() /*-{
    return this.selectionStart;
  }-*/;

  public final native void setSelectionStart(int param_selectionStart) /*-{
    this.selectionStart = param_selectionStart;
  }-*/;

  public final native int getTextLength() /*-{
    return this.textLength;
  }-*/;

  public final native String getType() /*-{
    return this.type;
  }-*/;

  public final native String getValidationMessage() /*-{
    return this.validationMessage;
  }-*/;

  public final native JsValidityState getValidity() /*-{
    return this.validity;
  }-*/;

  public final native String getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(String param_value) /*-{
    this.value = param_value;
  }-*/;

  public final native boolean isWillValidate() /*-{
    return this.willValidate;
  }-*/;

  public final native String getWrap() /*-{
    return this.wrap;
  }-*/;

  public final native void setWrap(String param_wrap) /*-{
    this.wrap = param_wrap;
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native void select() /*-{
    this.select();
  }-*/;

  public final native void setCustomValidity(String error) /*-{
    this.setCustomValidity(error);
  }-*/;

  public final native void setSelectionRange(int start, int end) /*-{
    this.setSelectionRange(start, end);
  }-*/;

  public final native void setSelectionRange(int start, int end, String direction) /*-{
    this.setSelectionRange(start, end, direction);
  }-*/;
}
