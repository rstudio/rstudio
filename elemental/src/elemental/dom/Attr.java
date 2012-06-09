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
  * <p>This type represents a DOM&nbsp;element's attribute as an object. In most DOM methods, you will probably directly retrieve the attribute as a string (e.g., <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Element.getAttribute">Element.getAttribute()</a></code>
, but certain functions (e.g., <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Element.getAttributeNode">Element.getAttributeNode()</a></code>
)&nbsp;or means of iterating give <code>Attr</code> types.</p>
<div class="warning"><strong>Warning:</strong> In DOM Core 1, 2 and 3, Attr inherited from Node. This is no longer the case in <a class="external" rel="external" href="http://www.w3.org/TR/dom/" title="http://www.w3.org/TR/dom/" target="_blank">DOM4</a>. In order to bring the implementation of <code>Attr</code> up to specification, work is underway to change it to no longer inherit from <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
. You should not be using any <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>
 properties or methods on <code>Attr</code> objects. Starting in Gecko 7.0 (Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
, the ones that are going to be removed output warning messages to the console. You should revise your code accordingly. See <a rel="custom" href="https://developer.mozilla.org/en/DOM/Attr#Deprecated_properties_and_methods">Deprecated properties and methods</a> for a complete list.</div>
  */
public interface Attr extends Node {


  /**
    * Indicates whether the attribute is an "ID attribute". An "ID attribute" being an attribute which value is expected to be unique across a DOM Document. In HTML DOM, "id" is the only ID attribute, but XML documents could define others. Whether or not an attribute is unique is often determined by a DTD or other schema description.
    */
  boolean isId();


  /**
    * The attribute's name.
    */
  String getName();


  /**
    * This property has been deprecated and will be removed in the future. Since you can only get Attr objects from elements, you should already know th
    */
  Element getOwnerElement();


  /**
    * This property has been deprecated and will be removed in the future; it now always returns <code>true</code>.
    */
  boolean isSpecified();


  /**
    * The attribute's value.
    */
  String getValue();

  void setValue(String arg);
}
