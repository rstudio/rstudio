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
  * <p>DocumentFragment has no properties or methods of its own, but inherits from <a title="En/DOM/Node" class="internal" rel="internal" href="https://developer.mozilla.org/En/DOM/Node"><code>Node</code></a>. </p>
<p>A <code><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-B63ED1A3" title="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-B63ED1A3" target="_blank">DocumentFragment</a></code> is a minimal document object that has no parent. It is used as a light-weight version of document to store well-formed or potentially non-well-formed fragments of XML.</p>
<p>See <a title="En/DOM/Node" class="internal" rel="internal" href="https://developer.mozilla.org/En/DOM/Node"><code>Node</code></a> for a listing of its properties, constants and methods.</p>
<p>Various other methods can take a document fragment as an argument (e.g., any <code><a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1950641247" title="http://www.w3.org/TR/DOM-Level-2-Core/core.html#ID-1950641247" target="_blank">Node</a></code> interface methods such as <code><a title="En/DOM/Node.appendChild" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.appendChild">appendChild</a></code> and <code><a title="En/DOM/Node.insertBefore" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.insertBefore">insertBefore</a></code>), in which case the children of the fragment are appended or inserted, not the fragment itself.</p>
  */
public interface DocumentFragment extends Node, NodeSelector {

  Element querySelector(String selectors);

  NodeList querySelectorAll(String selectors);
}
