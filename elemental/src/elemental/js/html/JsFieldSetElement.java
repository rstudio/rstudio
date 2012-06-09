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
import elemental.html.HTMLCollection;
import elemental.html.FormElement;
import elemental.html.FieldSetElement;

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

public class JsFieldSetElement extends JsElement  implements FieldSetElement {
  protected JsFieldSetElement() {}

  public final native boolean isDisabled() /*-{
    return this.disabled;
  }-*/;

  public final native void setDisabled(boolean param_disabled) /*-{
    this.disabled = param_disabled;
  }-*/;

  public final native JsHTMLCollection getElements() /*-{
    return this.elements;
  }-*/;

  public final native JsFormElement getForm() /*-{
    return this.form;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
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

  public final native boolean isWillValidate() /*-{
    return this.willValidate;
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native void setCustomValidity(String error) /*-{
    this.setCustomValidity(error);
  }-*/;
}
