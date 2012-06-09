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
  * DOM heading elements expose the <a title="http://www.w3.org/TR/html5/sections.html#htmlheadingelement" class=" external" rel="external nofollow" href="http://www.w3.org/TR/html5/sections.html#htmlheadingelement" target="_blank">HTMLHeadingElement</a> (or <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" rel="external nofollow" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-43345119" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-43345119" target="_blank"><code>HTMLHeadingElement</code></a>) interface. In <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML/HTML5">HTML 5</a></span>, this interface inherits from HTMLElement, but defines no additional members, though in <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> it introduces the deprecated <code>align</code> property.
  */
public interface HeadingElement extends Element {


  /**
    * Enumerated attribute indicating alignment of the heading with respect to the surrounding context.
    */
  String getAlign();

  void setAlign(String arg);
}
