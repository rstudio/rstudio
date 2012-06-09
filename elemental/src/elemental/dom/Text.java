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
  * <p>In the <a title="en/DOM" rel="internal" href="https://developer.mozilla.org/en/DOM">DOM</a>, the Text interface represents the textual content of an <a class="internal" title="En/DOM/Element" rel="internal" href="https://developer.mozilla.org/en/DOM/element">Element</a> or <a class="internal" title="En/DOM/Attr" rel="internal" href="https://developer.mozilla.org/En/DOM/Attr">Attr</a>.&nbsp; If an element has no markup within its content, it has a single child implementing Text that contains the element's text.&nbsp; However, if the element contains markup, it is parsed into information items and Text nodes that form its children.</p>
<p>New documents have a single Text node for each block of text.&nbsp; Over time, more Text nodes may be created as the document's content changes.&nbsp; The <code>Node.normalize()</code>&nbsp;method merges adjacent Text objects back into a single node for each block of text.</p>
<p>Text also implements the <a title="En/DOM/CharacterData" rel="internal" href="https://developer.mozilla.org/En/DOM/CharacterData">CharacterData</a> interface (which implements the Node interface).</p>
  */
public interface Text extends CharacterData {


  /**
    * Returns all text of all Text nodes logically adjacent to this node, concatenated in document order.
    */
  String getWholeText();


  /**
    * Replaces the text of the current node and all logically adjacent nodes with the specified text. <div class="note"><strong>Note: </strong>Do not use this method as it has been removed from the standard and is no longer implemented in recent browsers, like Firefox 10.</div>
    */
  Text replaceWholeText(String content);


  /**
    * Breaks the node into two nodes at a specified offset.
    */
  Text splitText(int offset);
}
