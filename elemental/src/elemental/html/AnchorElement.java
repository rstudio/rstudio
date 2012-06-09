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
  * DOM anchor elements expose the <a target="_blank" href="http://www.w3.org/TR/html5/text-level-semantics.html#htmlanchorelement" rel="external nofollow" class=" external" title="http://www.w3.org/TR/html5/text-level-semantics.html#htmlanchorelement">HTMLAnchorElement</a> (or <span><a href="https://developer.mozilla.org/en/HTML" rel="custom nofollow">HTML 4</a></span> <a target="_blank" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-48250443" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-48250443" rel="external nofollow" class=" external"><code>HTMLAnchorElement</code></a>) interface, which provides special properties and methods (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object interface they also have available to them by inheritance) for manipulating the layout and presentation of hyperlink elements.
  */
public interface AnchorElement extends Element {


  /**
    * The character encoding of the linked resource. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getCharset();

  void setCharset(String arg);


  /**
    * Comma-separated list of coordinates. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getCoords();

  void setCoords(String arg);

  String getDownload();

  void setDownload(String arg);


  /**
    * The fragment identifier (including the leading hash mark (#)), if any, in the referenced URL.
    */
  String getHash();

  void setHash(String arg);


  /**
    * The hostname and port (if it's not the default port) in the referenced URL.
    */
  String getHost();

  void setHost(String arg);


  /**
    * The hostname in the referenced URL.
    */
  String getHostname();

  void setHostname(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/a#attr-href">href</a></code>
 HTML attribute, containing a valid URL of a linked resource.
    */
  String getHref();

  void setHref(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/a#attr-hreflang">hreflang</a></code>
 HTML&nbsp;attribute, indicating the language of the linked resource.
    */
  String getHreflang();

  void setHreflang(String arg);


  /**
    * Anchor name. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getName();

  void setName(String arg);

  String getOrigin();


  /**
    * The path name component, if any, of the referenced URL.
    */
  String getPathname();

  void setPathname(String arg);

  String getPing();

  void setPing(String arg);


  /**
    * The port component, if any, of the referenced URL.
    */
  String getPort();

  void setPort(String arg);


  /**
    * The protocol component (including trailing colon (:)), of the referenced URL.
    */
  String getProtocol();

  void setProtocol(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/a#attr-rel">rel</a></code>
 HTML attribute, specifying the relationship of the target object to the link object.
    */
  String getRel();

  void setRel(String arg);


  /**
    * Reverse link type. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getRev();

  void setRev(String arg);


  /**
    * The search element (including leading question mark (?)), if any, of the referenced URL
    */
  String getSearch();

  void setSearch(String arg);


  /**
    * The shape of the active area. 

<span title="">Obsolete</span> in 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML/HTML5">HTML5</a></span>
    */
  String getShape();

  void setShape(String arg);


  /**
    * Reflectst the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/a#attr-target">target</a></code>
 HTML attribute, indicating where to display the linked resource.
    */
  String getTarget();

  void setTarget(String arg);


  /**
    * Same as the <strong><a title="https://developer.mozilla.org/En/DOM/Node.textContent" rel="internal" href="https://developer.mozilla.org/En/DOM/Node.textContent">textContent</a></strong> property.
    */
  String getText();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/a#attr-type">type</a></code>
 HTML attribute, indicating the MIME type of the linked resource.
    */
  String getType();

  void setType(String arg);
}
