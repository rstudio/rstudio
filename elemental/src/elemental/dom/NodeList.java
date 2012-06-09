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
  * NodeList objects are collections of nodes returned by <a title="document.getElementsByTagName" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByTagName"><code>getElementsByTagName</code></a>, <a title="document.getElementsByTagNameNS" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByTagNameNS"><code>getElementsByTagNameNS</code></a>, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.childNodes">Node.childNodes</a></code>
, <a title="document.querySelectorAll" rel="internal" href="https://developer.mozilla.org/En/DOM/Document.querySelectorAll">querySelectorAll</a>, <a title="document.getElementsByClassName" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByClassName"><code>getElementsByClassName</code></a>, etc.NodeList objects are collections of nodes returned by <a title="document.getElementsByTagName" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByTagName"><code>getElementsByTagName</code></a>, <a title="document.getElementsByTagNameNS" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByTagNameNS"><code>getElementsByTagNameNS</code></a>, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node.childNodes">Node.childNodes</a></code>
, <a title="document.querySelectorAll" rel="internal" href="https://developer.mozilla.org/En/DOM/Document.querySelectorAll">querySelectorAll</a>, <a title="document.getElementsByClassName" rel="internal" href="https://developer.mozilla.org/en/DOM/document.getElementsByClassName"><code>getElementsByClassName</code></a>, etc.
  */
public interface NodeList extends Indexable {


  /**
    * Reflects the number of elements in the NodeList.&nbsp;
    */
  int getLength();


  /**
    * Returns an item in the list by its index, or <code>null</code> if out-of-bounds. Equivalent to nodeList[idx]
    */
  Node item(int index);
}
