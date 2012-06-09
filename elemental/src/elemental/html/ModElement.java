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
  * DOM mod (modification)&nbsp;objects expose the <a title="http://www.w3.org/TR/html5/edits.html#htmlmodelement" class=" external" rel="external nofollow" href="http://www.w3.org/TR/html5/edits.html#htmlmodelement" target="_blank">HTMLModElement</a> (or <span><a rel="custom nofollow" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" rel="external nofollow" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-79359609" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-79359609" target="_blank"><code>HTMLModElement</code></a>) interface, which provides special properties (beyond the regular <a rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> object interface they also have available to them by inheritance) for manipulating modification elements.
  */
public interface ModElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/del#attr-cite">cite</a></code>
 HTML attribute, containing a URI of a resource explaining the change.
    */
  String getCite();

  void setCite(String arg);

  String getDateTime();

  void setDateTime(String arg);
}
