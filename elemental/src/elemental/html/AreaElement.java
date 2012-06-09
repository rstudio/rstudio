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
  * DOM area objects expose the <a class=" external" title="http://www.w3.org/TR/html5/the-map-element.html#htmlareaelement" rel="external" href="http://www.w3.org/TR/html5/the-map-element.html#htmlareaelement" target="_blank">HTMLAreaElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a class=" external" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-26019118" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-26019118" target="_blank"><code>HTMLAreaElement</code></a>) interface, which provides special properties and methods (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of area elements.
  */
public interface AreaElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/area#attr-alt">alt</a></code>
 HTML attribute, containing alternative text for the element.
    */
  String getAlt();

  void setAlt(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/area#attr-coords">coords</a></code>
 HTML attribute, containing coordinates to define the hot-spot region.
    */
  String getCoords();

  void setCoords(String arg);


  /**
    * The fragment identifier (including the leading hash mark (#)), if any, in the referenced URL.
    */
  String getHash();


  /**
    * The hostname and port (if it's not the default port) in the referenced URL.
    */
  String getHost();


  /**
    * The hostname in the referenced URL.
    */
  String getHostname();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/area#attr-href">href</a></code>
 HTML attribute, containing a valid URL&nbsp;of a linked resource.
    */
  String getHref();

  void setHref(String arg);


  /**
    * Indicates that this area is inactive. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  boolean isNoHref();

  void setNoHref(boolean arg);


  /**
    * The path name component, if any, of the referenced URL.
    */
  String getPathname();

  String getPing();

  void setPing(String arg);


  /**
    * The port component, if any, of the referenced URL.
    */
  String getPort();


  /**
    * The protocol component (including trailing colon (:)), of the referenced URL.
    */
  String getProtocol();


  /**
    * The search element (including leading question mark (?)), if any, of the referenced URL
    */
  String getSearch();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/area#attr-shape">shape</a></code>
 HTML&nbsp;attribute, indicating the shape of the hot-spot, limited to known values.
    */
  String getShape();

  void setShape(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/area#attr-target">target</a></code>
 HTML&nbsp;attribute, indicating the browsing context in which to open the linked resource.
    */
  String getTarget();

  void setTarget(String arg);
}
