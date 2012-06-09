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
import elemental.events.EventListener;
import elemental.events.EventTarget;
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * A <code>Node</code> is an interface from which a number of DOM types inherit, and allows these various types to be treated (or tested) similarly.<br> The following all inherit this interface and its methods and properties (though they may return null in particular cases where not relevant; or throw an exception when adding children to a node type for which no children can exist): <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Element">Element</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Attr">Attr</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/CharacterData">CharacterData</a></code>
 (which <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Text">Text</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Comment">Comment</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/CDATASection">CDATASection</a></code>
 inherit), <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/ProcessingInstruction">ProcessingInstruction</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentFragment">DocumentFragment</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentType">DocumentType</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Notation">Notation</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Entity">Entity</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/EntityReference">EntityReference</a></code>
  */
public interface Node extends EventTarget {

    static final int ATTRIBUTE_NODE = 2;

    static final int CDATA_SECTION_NODE = 4;

    static final int COMMENT_NODE = 8;

    static final int DOCUMENT_FRAGMENT_NODE = 11;

    static final int DOCUMENT_NODE = 9;

    static final int DOCUMENT_POSITION_CONTAINED_BY = 0x10;

    static final int DOCUMENT_POSITION_CONTAINS = 0x08;

    static final int DOCUMENT_POSITION_DISCONNECTED = 0x01;

    static final int DOCUMENT_POSITION_FOLLOWING = 0x04;

    static final int DOCUMENT_POSITION_IMPLEMENTATION_SPECIFIC = 0x20;

    static final int DOCUMENT_POSITION_PRECEDING = 0x02;

    static final int DOCUMENT_TYPE_NODE = 10;

    static final int ELEMENT_NODE = 1;

    static final int ENTITY_NODE = 6;

    static final int ENTITY_REFERENCE_NODE = 5;

    static final int NOTATION_NODE = 12;

    static final int PROCESSING_INSTRUCTION_NODE = 7;

    static final int TEXT_NODE = 3;

  NamedNodeMap getAttributes();

  String getBaseURI();

  NodeList getChildNodes();

  Node getFirstChild();

  Node getLastChild();

  String getLocalName();

  String getNamespaceURI();

  Node getNextSibling();

  String getNodeName();

  int getNodeType();

  String getNodeValue();

  void setNodeValue(String arg);

  Document getOwnerDocument();

  Element getParentElement();

  Node getParentNode();

  String getPrefix();

  void setPrefix(String arg);

  Node getPreviousSibling();

  String getTextContent();

  void setTextContent(String arg);

  EventRemover addEventListener(String type, EventListener listener);

  EventRemover addEventListener(String type, EventListener listener, boolean useCapture);

  Node appendChild(Node newChild);

  Node cloneNode(boolean deep);

  int compareDocumentPosition(Node other);

  boolean contains(Node other);

  boolean dispatchEvent(Event event);

  boolean hasAttributes();

  boolean hasChildNodes();

  Node insertBefore(Node newChild, Node refChild);

  boolean isDefaultNamespace(String namespaceURI);

  boolean isEqualNode(Node other);

  boolean isSameNode(Node other);

  boolean isSupported(String feature, String version);

  String lookupNamespaceURI(String prefix);

  String lookupPrefix(String namespaceURI);

  void normalize();

  Node removeChild(Node oldChild);

  void removeEventListener(String type, EventListener listener);

  void removeEventListener(String type, EventListener listener, boolean useCapture);

  Node replaceChild(Node newChild, Node oldChild);
}
