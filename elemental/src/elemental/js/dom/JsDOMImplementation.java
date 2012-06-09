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
import elemental.dom.DocumentType;
import elemental.js.css.JsCSSStyleSheet;
import elemental.css.CSSStyleSheet;
import elemental.dom.DOMImplementation;
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

public class JsDOMImplementation extends JsElementalMixinBase  implements DOMImplementation {
  protected JsDOMImplementation() {}

  public final native JsCSSStyleSheet createCSSStyleSheet(String title, String media) /*-{
    return this.createCSSStyleSheet(title, media);
  }-*/;

  public final native JsDocument createDocument(String namespaceURI, String qualifiedName, DocumentType doctype) /*-{
    return this.createDocument(namespaceURI, qualifiedName, doctype);
  }-*/;

  public final native JsDocumentType createDocumentType(String qualifiedName, String publicId, String systemId) /*-{
    return this.createDocumentType(qualifiedName, publicId, systemId);
  }-*/;

  public final native JsDocument createHTMLDocument(String title) /*-{
    return this.createHTMLDocument(title);
  }-*/;

  public final native boolean hasFeature(String feature, String version) /*-{
    return this.hasFeature(feature, version);
  }-*/;
}
