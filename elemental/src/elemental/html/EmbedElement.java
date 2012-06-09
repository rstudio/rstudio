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
import elemental.svg.SVGDocument;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <strong>Note:</strong>&nbsp;This topic describes the HTMLEmbedElement interface as defined in the HTML5 standard. It does not address earlier, non-standardized version of the interface.
  */
public interface EmbedElement extends Element {

  String getAlign();

  void setAlign(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/embed#attr-height">height</a></code>
 HTML&nbsp;attribute, containing the displayed height of the resource.
    */
  String getHeight();

  void setHeight(String arg);

  String getName();

  void setName(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/embed#attr-src">src</a></code>
 HTML&nbsp;attribute, containing the address of the resource.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/embed#attr-type">type</a></code>
 HTML&nbsp;attribute, containing the type of the resource.
    */
  String getType();

  void setType(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/embed#attr-width">width</a></code>
 HTML&nbsp;attribute, containing the displayed width of the resource.
    */
  String getWidth();

  void setWidth(String arg);

  SVGDocument getSVGDocument();
}
