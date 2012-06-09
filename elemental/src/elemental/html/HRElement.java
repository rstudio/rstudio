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
  * DOM <code>hr</code> elements expose the <a target="_blank" rel="external nofollow" class=" external" title="http://www.w3.org/TR/html5/grouping-content.html#htmlhrelement" href="http://www.w3.org/TR/html5/grouping-content.html#htmlhrelement">HTMLHRElement</a> (or <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-68228811" rel="external nofollow" class=" external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-68228811"><code>HTMLHRElement</code></a>) interface, which provides special properties (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating <code>hr</code> elements. In <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML/HTML5">HTML 5</a></span>, this interface inherits from HTMLElement, but defines no additional members.
  */
public interface HRElement extends Element {


  /**
    * Enumerated attribute indicating alignment of the rule with respect to the surrounding context.
    */
  String getAlign();

  void setAlign(String arg);

  boolean isNoShade();

  void setNoShade(boolean arg);


  /**
    * The height of the rule.
    */
  String getSize();

  void setSize(String arg);


  /**
    * The width of the rule on the page.
    */
  String getWidth();

  void setWidth(String arg);
}
