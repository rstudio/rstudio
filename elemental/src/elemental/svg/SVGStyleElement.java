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
package elemental.svg;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The <code>SVGStyleElement</code> interface corresponds to the SVG <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/style">&lt;style&gt;</a></code>
 element.
  */
public interface SVGStyleElement extends SVGElement, SVGLangSpace {

  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/media" class="new">media</a></code> on the given element. A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 is raised with code <code>NO_MODIFICATION_ALLOWED_ERR</code> on an attempt to change the value of a read only attribut.
    */
  String getMedia();

  void setMedia(String arg);


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/title" class="new">title</a></code> on the given element. A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 is raised with code <code>NO_MODIFICATION_ALLOWED_ERR</code> on an attempt to change the value of a read only attribut.
    */
  String getTitle();

  void setTitle(String arg);


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/type" class="new">type</a></code> on the given element. A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 is raised with code <code>NO_MODIFICATION_ALLOWED_ERR</code> on an attempt to change the value of a read only attribut.
    */
  String getType();

  void setType(String arg);
}
