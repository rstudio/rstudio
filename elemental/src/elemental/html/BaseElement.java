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
  * The <code>base</code> object exposes the <a class=" external" title="http://www.w3.org/TR/html5/semantics.html#htmlbaseelement" rel="external" href="http://www.w3.org/TR/html5/semantics.html#htmlbaseelement" target="_blank">HTMLBaseElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class="external" target="_blank" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-73629039" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-73629039">HTMLBaseElement</a>) interface which contains the base URI&nbsp;for a document.&nbsp; This object inherits all of the properties and methods as described in the <a class="internal" title="en/DOM/element" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> section.
  */
public interface BaseElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/base#attr-href">href</a></code>
 HTML attribute, containing a base URL for relative URLs in the document.
    */
  String getHref();

  void setHref(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/base#attr-target">target</a></code>
 HTML attribute, containing a default target browsing context or frame for elements that do not have a target reference specified.
    */
  String getTarget();

  void setTarget(String arg);
}
