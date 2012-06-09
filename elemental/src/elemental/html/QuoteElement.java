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
  * DOM quote objects expose the <a class=" external" title="http://www.w3.org/TR/html5/grouping-content.html#htmlquoteelement" rel="external" href="http://www.w3.org/TR/html5/grouping-content.html#htmlquoteelement" target="_blank">HTMLQuoteElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-70319763" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-70319763" target="_blank"><code>HTMLQuoteElement</code></a>) interface, which provides special properties&nbsp; (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating quote elements.
  */
public interface QuoteElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/blockquote#attr-cite">cite</a></code>
 HTML attribute, containing a URL for the source of the quotation.
    */
  String getCite();

  void setCite(String arg);
}
