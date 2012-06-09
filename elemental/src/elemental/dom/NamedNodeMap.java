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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * A collection of nodes returned by <a title="En/DOM/Element.attributes" class="internal" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.attributes"><code>Element.attributes</code></a> (also potentially for <code><a title="En/DOM/DocumentType.entities" rel="internal" href="https://developer.mozilla.org/En/DOM/DocumentType.entities" class="new internal">DocumentType.entities</a></code>, <code><a title="En/DOM/DocumentType.notations" rel="internal" href="https://developer.mozilla.org/En/DOM/DocumentType.notations" class="new internal">DocumentType.notations</a></code>). <code>NamedNodeMap</code>s are not in any particular order (unlike <code><a title="En/DOM/NodeList" class="internal" rel="internal" href="https://developer.mozilla.org/En/DOM/NodeList">NodeList</a></code>), although they may be accessed by an index as in an array (they may also be accessed with the <code>item</code>() method). A NamedNodeMap object are live and will thus be auto-updated if changes are made to their contents internally or elsewhere.
  */
public interface NamedNodeMap extends Indexable {

  int getLength();

  Node getNamedItem(String name);

  Node getNamedItemNS(String namespaceURI, String localName);

  Node item(int index);

  Node removeNamedItem(String name);

  Node removeNamedItemNS(String namespaceURI, String localName);

  Node setNamedItem(Node node);

  Node setNamedItemNS(Node node);
}
