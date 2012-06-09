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
  * Interface <code>SVGTests</code> defines an interface which applies to all elements which have attributes 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/requiredFeatures">requiredFeatures</a></code>, 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/requiredExtensions" class="new">requiredExtensions</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/systemLanguage" class="new">systemLanguage</a></code>.
  */
public interface SVGTests {


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/requiredExtensions" class="new">requiredExtensions</a></code> on the given element.
    */
  SVGStringList getRequiredExtensions();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/requiredFeatures">requiredFeatures</a></code> on the given element.
    */
  SVGStringList getRequiredFeatures();


  /**
    * Corresponds to attribute 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/systemLanguage" class="new">systemLanguage</a></code> on the given element.
    */
  SVGStringList getSystemLanguage();


  /**
    * Returns true if the browser supports the given extension, specified by a URI.
    */
  boolean hasExtension(String extension);
}
