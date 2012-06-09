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
package elemental.js.xml;
import elemental.dom.Node;
import elemental.xml.XSLTProcessor;
import elemental.js.dom.JsDocument;
import elemental.dom.DocumentFragment;
import elemental.js.dom.JsDocumentFragment;
import elemental.js.dom.JsNode;
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

public class JsXSLTProcessor extends JsElementalMixinBase  implements XSLTProcessor {
  protected JsXSLTProcessor() {}

  public final native void clearParameters() /*-{
    this.clearParameters();
  }-*/;

  public final native String getParameter(String namespaceURI, String localName) /*-{
    return this.getParameter(namespaceURI, localName);
  }-*/;

  public final native void importStylesheet(Node stylesheet) /*-{
    this.importStylesheet(stylesheet);
  }-*/;

  public final native void removeParameter(String namespaceURI, String localName) /*-{
    this.removeParameter(namespaceURI, localName);
  }-*/;

  public final native void reset() /*-{
    this.reset();
  }-*/;

  public final native void setParameter(String namespaceURI, String localName, String value) /*-{
    this.setParameter(namespaceURI, localName, value);
  }-*/;

  public final native JsDocument transformToDocument(Node source) /*-{
    return this.transformToDocument(source);
  }-*/;

  public final native JsDocumentFragment transformToFragment(Node source, Document docVal) /*-{
    return this.transformToFragment(source, docVal);
  }-*/;
}
