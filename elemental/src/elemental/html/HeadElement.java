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
  * The DOM <code>head</code> element exposes the <a title="http://www.w3.org/TR/html5/semantics.html#htmlheadelement" class=" external" rel="external" href="http://www.w3.org/TR/html5/semantics.html#htmlheadelement" target="_blank">HTMLHeadElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span>&nbsp; <a class=" external" target="_blank" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-77253168" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-77253168">HTMLHeadElement</a>) interface, which contains the descriptive information, or metadata, for a document. This object inherits all of the properties and methods described in the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 section. In 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>, this interface inherits from HTMLElement, but defines no additional members.
  */
public interface HeadElement extends Element {


  /**
    * The URIs of one or more metadata profiles (white space separated). 

<span class="deprecatedInlineTemplate" title="(Firefox 4 / Thunderbird 3.3 / SeaMonkey 2.1)
">Deprecated since Gecko 2.0</span>

 

<span title="(Firefox 7.0 / Thunderbird 7.0 / SeaMonkey 2.4)
">Obsolete since Gecko 7.0</span>
    */
  String getProfile();

  void setProfile(String arg);
}
