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
  * <p>The HTML <em>unordered list</em> element (<code>&lt;ul&gt;</code>) represents an unordered list of items, namely a collection of items that do not have a numerical ordering, and their order in the list is meaningless. Typically, unordered-list items are displayed with a bullet, which can be of several forms, like a dot, a circle or a squared. The bullet style is not defined in the HTML description of the page, but in its associated CSS, using the <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/list-style-type">list-style-type</a></code>
 property.</p>
<p>There is no limitation to the depth and imbrication of lists defined with the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 elements.</p>
<div class="note"><strong>Usage note: </strong> The <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 both represent a list of items. They differ in the way that, with the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 element, the order is meaningful. As a rule of thumb to determine which one to use, try changing the order of the list items; if the meaning is changed, the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 element should be used, else the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 is adequate.</div>
  */
public interface UListElement extends Element {


  /**
    * This Boolean attribute hints that the list should be rendered in a compact style. The interpretation of this attribute depends on the user agent and it doesn't work in all browsers. <div class="note"><strong>Usage note:&nbsp;</strong>Do not use this attribute, as it has been deprecated: the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 element should be styled using <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a>. To give a similar effect than the <span>compact</span> attribute, the <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a> property <a title="en/CSS/line-height" rel="internal" href="https://developer.mozilla.org/en/CSS/line-height">line-height</a> can be used with a value of <span>80%</span>.</div>
    */
  boolean isCompact();

  void setCompact(boolean arg);


  /**
    * Used to set the bullet style for the list. The values defined under <a title="en/HTML3.2" rel="internal" href="https://developer.mozilla.org/en/HTML3.2" class="new ">HTML3.2</a> and the transitional version of <a title="en/HTML4.01" rel="internal" href="https://developer.mozilla.org/en/HTML4.01" class="new ">HTML 4.0/4.01</a> are<span>:</span> <ul> <li><code>circle</code>,</li> <li><code>disc</code>,</li> <li>and <code>square</code>.</li> </ul> <p>A fourth bullet type has been defined in the WebTV interface, but not all browsers support it: <code>triangle.</code></p> <p>If not present and if no <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a> <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/list-style-type">list-style-type</a></code>
 property does apply to the element, the user agent decide to use a kind of bullets depending on the nesting level of the list.</p> <div class="note"><strong>Usage note:</strong> Do not use this attribute, as it has been deprecated: use the <a title="en/CSS" rel="internal" href="https://developer.mozilla.org/en/CSS">CSS</a> <code><a rel="custom" href="https://developer.mozilla.org/en/CSS/list-style-type">list-style-type</a></code>
 property instead.</div>
    */
  String getType();

  void setType(String arg);
}
