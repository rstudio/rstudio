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
import elemental.html.FormElement;
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

public class JsFormElement extends JsElement  implements FormElement {
  protected JsFormElement() {}

  public final native String getAcceptCharset() /*-{
    return this.acceptCharset;
  }-*/;

  public final native void setAcceptCharset(String param_acceptCharset) /*-{
    this.acceptCharset = param_acceptCharset;
  }-*/;

  public final native String getAction() /*-{
    return this.action;
  }-*/;

  public final native void setAction(String param_action) /*-{
    this.action = param_action;
  }-*/;

  public final native String getAutocomplete() /*-{
    return this.autocomplete;
  }-*/;

  public final native void setAutocomplete(String param_autocomplete) /*-{
    this.autocomplete = param_autocomplete;
  }-*/;

  public final native JsHTMLCollection getElements() /*-{
    return this.elements;
  }-*/;

  public final native String getEncoding() /*-{
    return this.encoding;
  }-*/;

  public final native void setEncoding(String param_encoding) /*-{
    this.encoding = param_encoding;
  }-*/;

  public final native String getEnctype() /*-{
    return this.enctype;
  }-*/;

  public final native void setEnctype(String param_enctype) /*-{
    this.enctype = param_enctype;
  }-*/;

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native String getMethod() /*-{
    return this.method;
  }-*/;

  public final native void setMethod(String param_method) /*-{
    this.method = param_method;
  }-*/;

  public final native String getName() /*-{
    return this.name;
  }-*/;

  public final native void setName(String param_name) /*-{
    this.name = param_name;
  }-*/;

  public final native boolean isNoValidate() /*-{
    return this.noValidate;
  }-*/;

  public final native void setNoValidate(boolean param_noValidate) /*-{
    this.noValidate = param_noValidate;
  }-*/;

  public final native String getTarget() /*-{
    return this.target;
  }-*/;

  public final native void setTarget(String param_target) /*-{
    this.target = param_target;
  }-*/;

  public final native boolean checkValidity() /*-{
    return this.checkValidity();
  }-*/;

  public final native void reset() /*-{
    this.reset();
  }-*/;

  public final native void submit() /*-{
    this.submit();
  }-*/;
}
