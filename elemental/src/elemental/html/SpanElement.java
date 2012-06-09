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
  * This HTML element is a generic inline container for phrasing content, which does not inherently represent anything. It can be used to group elements for styling purposes (using the <strong>class</strong> or <strong>id</strong> attributes), or because they share attribute values, such as <strong>lang</strong>. It should be used only when no other semantic element is appropriate. &lt;span&gt; is very much like a <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/div">&lt;div&gt;</a></code>
 element, but <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/div">&lt;div&gt;</a></code>
 is a block-level element whereas a &lt;span&gt; is an inline element.
  */
public interface SpanElement extends Element {
}
