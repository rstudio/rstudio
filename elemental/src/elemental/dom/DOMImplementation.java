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
package elemental.dom;
import elemental.css.CSSStyleSheet;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Provides methods which are not dependent on any particular DOM instances. Returned by <code><a title="En/DOM/Document.implementation" class="internal" rel="internal" href="https://developer.mozilla.org/en/DOM/document.implementation">document.implementation</a></code>.
  */
public interface DOMImplementation {

  CSSStyleSheet createCSSStyleSheet(String title, String media);

  Document createDocument(String namespaceURI, String qualifiedName, DocumentType doctype);

  DocumentType createDocumentType(String qualifiedName, String publicId, String systemId);

  Document createHTMLDocument(String title);

  boolean hasFeature(String feature, String version);
}
