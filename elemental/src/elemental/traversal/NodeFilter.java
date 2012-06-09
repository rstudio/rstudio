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
package elemental.traversal;
import elemental.dom.Node;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface NodeFilter {

  /**
    * Value returned by the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/NodeFilter.acceptNode" class="new">NodeFilter.acceptNode()</a></code>
 method when a node should be accepted.
    */

    static final short FILTER_ACCEPT = 1;

  /**
    * Value to be returned by the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/NodeFilter.acceptNode" class="new">NodeFilter.acceptNode()</a></code>
 method when a node should be rejected. The children of rejected nodes are not visited by the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
 object; this value is treated as "skip this node and all its children".
    */

    static final short FILTER_REJECT = 2;

  /**
    * Value to be returned by <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/DOM/NodeFilter.acceptNode" class="new">NodeFilter.acceptNode()</a></code>
 for nodes to be skipped by the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
 object. The children of skipped nodes are still considered. This is treated as "skip this node but not its children".
    */

    static final short FILTER_SKIP = 3;

  /**
    * Shows all nodes.
    */

    static final int SHOW_ALL = 0xFFFFFFFF;

  /**
    * Shows attribute <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Attr">Attr</a></code>
 nodes. This is meaningful only when creating a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
 with an <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Attr">Attr</a></code>
 node as its root; in this case, it means that the attribute node will appear in the first position of the iteration or traversal. Since attributes are never children of other nodes, they do not appear when traversing over the document tree.
    */

    static final int SHOW_ATTRIBUTE = 0x00000002;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/CDATASection">CDATASection</a></code>
&nbsp;nodes.
    */

    static final int SHOW_CDATA_SECTION = 0x00000008;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Comment">Comment</a></code>
&nbsp;nodes.
    */

    static final int SHOW_COMMENT = 0x00000080;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Document">Document</a></code>
&nbsp;nodes.
    */

    static final int SHOW_DOCUMENT = 0x00000100;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentFragment">DocumentFragment</a></code>
&nbsp;nodes.
    */

    static final int SHOW_DOCUMENT_FRAGMENT = 0x00000400;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DocumentType">DocumentType</a></code>
&nbsp;nodes.
    */

    static final int SHOW_DOCUMENT_TYPE = 0x00000200;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Element">Element</a></code>
&nbsp;nodes.
    */

    static final int SHOW_ELEMENT = 0x00000001;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Entity">Entity</a></code>
&nbsp;nodes. This is meaningful only when creating a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
 with an <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Entity">Entity</a></code>
 node as its root; in this case, it means that the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Entity">Entity</a></code>
 node will appear in the first position of the traversal. Since entities are not part of the document tree, they do not appear when traversing over the document tree.
    */

    static final int SHOW_ENTITY = 0x00000020;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/EntityReference">EntityReference</a></code>
&nbsp;nodes.
    */

    static final int SHOW_ENTITY_REFERENCE = 0x00000010;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Notation">Notation</a></code>
 nodes. This is meaningful only when creating a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
 with a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Notation">Notation</a></code>
 node as its root; in this case, it means that the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Notation">Notation</a></code>
 node will appear in the first position of the traversal. Since entities are not part of the document tree, they do not appear when traversing over the document tree.
    */

    static final int SHOW_NOTATION = 0x00000800;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/ProcessingInstruction">ProcessingInstruction</a></code>
&nbsp;nodes.
    */

    static final int SHOW_PROCESSING_INSTRUCTION = 0x00000040;

  /**
    * Shows <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Text">Text</a></code>
&nbsp;nodes.
    */

    static final int SHOW_TEXT = 0x00000004;


  /**
    * The accept node method used by the filter is supplied as an object property when constructing the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/NodeIterator">NodeIterator</a></code>
 or <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TreeWalker">TreeWalker</a></code>
.
    */
  short acceptNode(Node n);
}
