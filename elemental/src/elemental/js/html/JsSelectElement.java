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
import elemental.html.OptionElement;
import elemental.html.HTMLOptionsCollection;
import elemental.html.HTMLCollection;
import elemental.html.FormElement;
import elemental.js.dom.JsNode;
import elemental.html.SelectElement;
import elemental.js.dom.JsNodeList;
import elemental.dom.NodeList;
import elemental.dom.Node;

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

public class JsSelectElement extends JsElement  implements SelectElement {
  protected JsSelectElement() {}

  public final native boolean isAutofocus() /*-{
    return this.autofocus;
  }-*/;

  public final native void setAutofocus(boolean param_autofocus) /*-{
    this.autofocus = param_autofocus;
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

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native void setLength(int param_length) /*-{
    this.length = param_length;
  }-*/;

  public final native boolean isMultiple() /*-{
    return this.multiple;
  }-*/;

  public final native void setMultiple(boolean param_multiple) /*-{
    this.multiple = param_multiple;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native JsHTMLOptionsCollection getOptions() /*-{
    return this.options;
  }-*/;

  public final native boolean isRequired() /*-{
    return this.required;
  }-*/;

  public final native void setRequired(boolean param_required) /*-{
    this.required = param_required;
  }-*/;

  public final native int getSelectedIndex() /*-{
    return this.selectedIndex;
  }-*/;

  public final native void setSelectedIndex(int param_selectedIndex) /*-{
    this.selectedIndex = param_selectedIndex;
  }-*/;

  public final native JsHTMLCollection getSelectedOptions() /*-{
    return this.selectedOptions;
  }-*/;

  public final native int getSize() /*-{
    return this.size;
  }-*/;

  public final native void setSize(int param_size) /*-{
    this.size = param_size;
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

  public final native void add(Element element, Element before) /*-{
    this.add(element, before);
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native JsNode item(int index) /*-{
    return this.item(index);
  }-*/;

  public final native JsNode namedItem(String name) /*-{
    return this.namedItem(name);
  }-*/;

  public final native void remove(int index) /*-{
    this.remove(index);
  }-*/;

  public final native void remove(OptionElement option) /*-{
    this.remove(option);
  }-*/;

  public final native void setCustomValidity(String error) /*-{
    this.setCustomValidity(error);
  }-*/;
}
