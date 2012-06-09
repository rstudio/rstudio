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
import elemental.stylesheets.StyleSheet;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>A processing instruction provides an opportunity for application-specific instructions to be embedded within XML and which can be ignored by XML processors which do not support processing their instructions (outside of their having a place in the DOM).</p>
<p>A Processing instruction is distinct from a <a title="en/XML/XML_Declaration" rel="internal" href="https://developer.mozilla.org/en/XML/XML_Declaration" class="new ">XML Declaration</a> which is used for other information about the document such as encoding and which appear (if it does) as the first item in the document.</p>
<p>User-defined processing instructions cannot begin with 'xml', as these are reserved (e.g., as used in &lt;?<a title="en/XML/xml-stylesheet" rel="internal" href="https://developer.mozilla.org/en/XML/xml-stylesheet" class="new ">xml-stylesheet</a>&nbsp;?&gt;).</p>
<p>Also inherits methods and properties from <a class="internal" title="En/DOM/Node" rel="internal" href="https://developer.mozilla.org/en/DOM/Node"><code>Node</code></a>.</p>
  */
public interface ProcessingInstruction extends Node {

  String getData();

  void setData(String arg);

  StyleSheet getSheet();

  String getTarget();
}
