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
package elemental.xml;
import elemental.dom.Node;
import elemental.dom.DocumentFragment;
import elemental.dom.Document;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>XSLTProcesor is an object providing an interface to XSLT engine in Mozilla. It is available to unprivileged JavaScript.</p>
<ul> <li><a title="en/Using_the_Mozilla_JavaScript_interface_to_XSL_Transformations" rel="internal" href="https://developer.mozilla.org/en/Using_the_Mozilla_JavaScript_interface_to_XSL_Transformations">Using the Mozilla JavaScript interface to XSL Transformations</a></li> <li><a title="en/The_XSLT//JavaScript_Interface_in_Gecko" rel="internal" href="https://developer.mozilla.org/en/The_XSLT%2F%2FJavaScript_Interface_in_Gecko">The XSLT/JavaScript Interface in Gecko</a></li>
</ul>
  */
public interface XSLTProcessor {

  void clearParameters();

  String getParameter(String namespaceURI, String localName);

  void importStylesheet(Node stylesheet);

  void removeParameter(String namespaceURI, String localName);

  void reset();

  void setParameter(String namespaceURI, String localName, String value);

  Document transformToDocument(Node source);

  DocumentFragment transformToFragment(Node source, Document docVal);
}
