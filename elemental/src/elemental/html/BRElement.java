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
  * DOM break elements expose the <a class="external" href="http://www.w3.org/TR/html5/text-level-semantics.html#the-br-element" rel="external nofollow" target="_blank" title="http://www.w3.org/TR/html5/text-level-semantics.html#the-br-element">HTMLBRElement</a> (or <span><a href="https://developer.mozilla.org/en/HTML" rel="custom nofollow">HTML 4</a></span> <a class="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-56836063" rel="external nofollow" target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-56836063"><code>HTMLBRElement</code></a>) interface which inherits from HTMLElement, but defines no additional members in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>. The introduced additional property is also deprecated in 
<span>HTML 4.01</span>.
  */
public interface BRElement extends Element {


  /**
    * Indicates flow of text around floating objects.
    */
  String getClear();

  void setClear(String arg);
}
