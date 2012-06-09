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
package elemental.js.dom;
import elemental.dom.Element;
import elemental.dom.ShadowRoot;
import elemental.js.html.JsSelection;
import elemental.dom.DocumentFragment;
import elemental.html.Selection;
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

public class JsShadowRoot extends JsDocumentFragment  implements ShadowRoot {
  protected JsShadowRoot() {}

  public final native JsElement getActiveElement() /*-{
    return this.activeElement;
  }-*/;

  public final native boolean isApplyAuthorStyles() /*-{
    return this.applyAuthorStyles;
  }-*/;

  public final native void setApplyAuthorStyles(boolean param_applyAuthorStyles) /*-{
    this.applyAuthorStyles = param_applyAuthorStyles;
  }-*/;

  public final native JsElement getHost() /*-{
    return this.host;
  }-*/;

  public final native String getInnerHTML() /*-{
    return this.innerHTML;
  }-*/;

  public final native void setInnerHTML(String param_innerHTML) /*-{
    this.innerHTML = param_innerHTML;
  }-*/;

  public final native JsElement getElementById(String elementId) /*-{
    return this.getElementById(elementId);
  }-*/;

  public final native JsNodeList getElementsByClassName(String className) /*-{
    return this.getElementsByClassName(className);
  }-*/;

  public final native JsNodeList getElementsByTagName(String tagName) /*-{
    return this.getElementsByTagName(tagName);
  }-*/;

  public final native JsNodeList getElementsByTagNameNS(String namespaceURI, String localName) /*-{
    return this.getElementsByTagNameNS(namespaceURI, localName);
  }-*/;

  public final native JsSelection getSelection() /*-{
    return this.getSelection();
  }-*/;
}
