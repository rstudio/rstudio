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
import elemental.dom.Element;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * All of the SVG DOM interfaces that correspond directly to elements in the SVG language derive from the <code>SVGElement</code> interface.
  */
public interface SVGElement extends Element {


  /**
    * The value of the 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/id" class="new">id</a></code> attribute on the given element, or the empty string if <code>id</code> is not present. 
    */
  String getId();

  void setId(String arg);


  /**
    * The nearest ancestor <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element. <code>Null</code> if the given element is the outermost svg element.
    */
  SVGSVGElement getOwnerSVGElement();


  /**
    * The element which established the current viewport. Often, the nearest ancestor <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element. <code>Null</code> if the given element is the outermost svg element.
    */
  SVGElement getViewportElement();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/xml%3Abase" class="new">xml:base</a></code> on the given element.
    */
  String getXmlbase();

  void setXmlbase(String arg);
}
