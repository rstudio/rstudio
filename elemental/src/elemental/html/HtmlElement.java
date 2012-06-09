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
  * <p>The <code>html</code> object exposes the <a class=" external" title="http://www.w3.org/TR/html5/semantics.html#htmlhtmlelement" rel="external" href="http://www.w3.org/TR/html5/semantics.html#htmlhtmlelement" target="_blank">HTMLHtmlElement</a> (
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a target="_blank" class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-33759296" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-33759296">HTMLHtmlElement</a>) interface and serves as the root node for a given HTML&nbsp;document.&nbsp; This object inherits the properties and methods described in the <a title="en/DOM/element" class="internal" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> section.&nbsp; In 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>, this interface inherits from HTMLElement, but provides no other members.</p>
<p>You can retrieve the <code>html</code> object for a document by obtaining the value of the <a class="internal" title="en/DOM/document.documentElement" rel="internal" href="https://developer.mozilla.org/en/DOM/document.documentElement"><code>document.documentElement</code></a> property.</p>
  */
public interface HtmlElement extends Element {

  String getManifest();

  void setManifest(String arg);


  /**
    * Version of the HTML&nbsp;Document Type Definition that governs this document.
    */
  String getVersion();

  void setVersion(String arg);
}
