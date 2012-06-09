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
import elemental.dom.Document;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM iframe objects expose the <a class="external" href="http://www.w3.org/TR/html5/the-iframe-element.html#htmliframeelement" rel="external nofollow" target="_blank" title="http://www.w3.org/TR/html5/the-iframe-element.html#htmliframeelement">HTMLIFrameElement</a> (or <span><a href="https://developer.mozilla.org/en/HTML" rel="custom nofollow">HTML 4</a></span> <a class="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-50708718" rel="external nofollow" target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-50708718"><code>HTMLIFrameElement</code></a>) interface, which provides special properties and methods (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of inline frame elements.
  */
public interface IFrameElement extends Element {


  /**
    * Specifies the alignment of the frame with respect to the surrounding context.
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * The active document in the inline frame's nested browsing context.
    */
  Document getContentDocument();


  /**
    * The window proxy for the nested browsing context.
    */
  Window getContentWindow();

  String getFrameBorder();

  void setFrameBorder(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe#attr-height">height</a></code>
 HTML&nbsp;attribute, indicating the height of the frame.
    */
  String getHeight();

  void setHeight(String arg);


  /**
    * URI of a long description of the frame.
    */
  String getLongDesc();

  void setLongDesc(String arg);


  /**
    * Height of the frame margin.
    */
  String getMarginHeight();

  void setMarginHeight(String arg);


  /**
    * Width of the frame margin.
    */
  String getMarginWidth();

  void setMarginWidth(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe#attr-name">name</a></code>
 HTML&nbsp;attribute, containing a name by which to refer to the frame.
    */
  String getName();

  void setName(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe#attr-sandbox">sandbox</a></code>
 HTML&nbsp;attribute, indicating extra restrictions on the behavior of the nested content.
    */
  String getSandbox();

  void setSandbox(String arg);


  /**
    * Indicates whether the browser should provide scrollbars for the frame.
    */
  String getScrolling();

  void setScrolling(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe#attr-src">src</a></code>
 HTML&nbsp;attribute, containing the address of the content to be embedded.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * The content to display in the frame.
    */
  String getSrcdoc();

  void setSrcdoc(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe#attr-width">width</a></code>
&nbsp;HTML&nbsp;attribute, indicating the width of the frame.
    */
  String getWidth();

  void setWidth(String arg);

  SVGDocument getSVGDocument();
}
