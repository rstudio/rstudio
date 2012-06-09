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
  * <p>The HTML <em>menu</em> element (<code>&lt;menu&gt;</code>) represents an unordered list of menu choices, or commands.</p>
<p>There is no limitation to the depth and nesting of lists defined with the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/menu">&lt;menu&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ol">&lt;ol&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 elements.</p>
<div class="note"><strong>Usage note: </strong> The <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/menu">&lt;menu&gt;</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 both represent an unordered list of items. They differ in the way that the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/ul">&lt;ul&gt;</a></code>
 element only contains items to display while the <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/menu">&lt;menu&gt;</a></code>
 element contains interactive items, to act on.</div>
<div class="note"><strong>Note</strong>: This element was deprecated in HTML4, but reintroduced in HTML5.</div>
  */
public interface MenuElement extends Element {

  boolean isCompact();

  void setCompact(boolean arg);
}
