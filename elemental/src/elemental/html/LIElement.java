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
import elemental.dom.Element;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The <em>HTML List item element</em> (<code>&lt;li&gt;</code>) is used to represent a list item. It should be contained in an ordered list (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
), an unordered list (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
) or a menu (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/menu">&lt;menu&gt;</a></code>
), where it represents a single entity in that list. In menus and unordered lists, list items are ordinarily displayed using bullet points. In order lists, they are usually displayed with some ascending counter on the left such as a number or letter
  */
public interface LIElement extends Element {


  /**
    * This character attributes indicates the numbering type: <ul> <li><code>a</code>: lowercase letters</li> <li><code>A</code>: uppercase letters</li> <li><code>i</code>: lowercase Roman numerals</li> <li><code>I</code>: uppercase Roman numerals</li> <li><code>1</code>: numbers</li> </ul> This type overrides the one used by its parent <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 element, if any.<br> <div class="note"><strong>Usage note:</strong> This attribute has been deprecated: use the CSS <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/list-style-type">list-style-type</a></code>
 property instead.</div>
    */
  String getType();

  void setType(String arg);


  /**
    * This integer attributes indicates the current ordinal value of the item in the list as defined by the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 element. The only allowed value for this attribute is a number, even if the list is displayed with Roman numerals or letters. List items that follow this one continue numbering from the value set. The <strong>value</strong> attribute has no meaning for unordered lists (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
) or for menus (<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/menu">&lt;menu&gt;</a></code>
). <div class="note"><strong>Note</strong>: This attribute was deprecated in HTML4, but reintroduced in HTML5.</div> <div class="geckoVersionNote"> <p>
</p><div class="geckoVersionHeading">Gecko 9.0 note<div>(Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
</div></div>
<p></p> <p>Prior to <span title="(Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
">Gecko&nbsp;9.0</span>, negative values were incorrectly converted to 0. Starting in <span title="(Firefox 9.0 / Thunderbird 9.0 / SeaMonkey 2.6)
">Gecko&nbsp;9.0</span> all integer values are correctly parsed.</p> </div>
    */
  int getValue();

  void setValue(int arg);
}
