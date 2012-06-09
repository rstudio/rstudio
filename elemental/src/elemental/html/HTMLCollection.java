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
package elemental.html;
import elemental.dom.Node;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p><code>HTMLCollection</code> is an interface representing a generic collection of elements (in document order) and offers methods and properties for traversing the list.</p>
<div class="note"><strong>Note:</strong> This interface is called <code>HTMLCollection</code> for historical reasons (before DOM4, collections implementing this interface could only have HTML elements as their items).</div>
<p><code>HTMLCollection</code>s in the HTML DOM are live; they are automatically updated when the underlying document is changed.</p>
  */
public interface HTMLCollection extends Indexable {


  /**
    * The number of items in the collection. <strong>Read only</strong>.
    */
  int getLength();


  /**
    * Returns the specific node at the given zero-based <code>index</code> into the list. Returns <code>null</code> if the <code>index</code> is out of range.
    */
  Node item(int index);


  /**
    * Returns the specific node whose ID or, as a fallback, name matches the string specified by <code>name</code>. Matching by name is only done as a last resort, only in HTML, and only if the referenced element supports the <code>name</code> attribute. Returns <code>null</code> if no node exists by the given name.
    */
  Node namedItem(String name);
}
