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
import elemental.html.OptionElement;
import elemental.html.FormElement;

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

public class JsOptionElement extends JsElement  implements OptionElement {
  protected JsOptionElement() {}

  public final native boolean isDefaultSelected() /*-{
    return this.defaultSelected;
  }-*/;

  public final native void setDefaultSelected(boolean param_defaultSelected) /*-{
    this.defaultSelected = param_defaultSelected;
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

  public final native int getIndex() /*-{
    return this.index;
  }-*/;

  public final native String getLabel() /*-{
    return this.label;
  }-*/;

  public final native void setLabel(String param_label) /*-{
    this.label = param_label;
  }-*/;

  public final native boolean isSelected() /*-{
    return this.selected;
  }-*/;

  public final native void setSelected(boolean param_selected) /*-{
    this.selected = param_selected;
  }-*/;

  public final native String getText() /*-{
    return this.text;
  }-*/;

  public final native void setText(String param_text) /*-{
    this.text = param_text;
  }-*/;

  public final native String getValue() /*-{
    return this.value;
  }-*/;

  public final native void setValue(String param_value) /*-{
    this.value = param_value;
  }-*/;
}
