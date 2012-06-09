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
  * DOM image objects expose the <a title="http://www.w3.org/TR/html5/embedded-content-1.html#htmlimageelement" class=" external" rel="external" href="http://www.w3.org/TR/html5/embedded-content-1.html#htmlimageelement" target="_blank">HTMLImageElement</a> (or 
<span><a rel="custom" href="https://developer.mozilla.org/en/HTML">HTML 4</a></span> <a title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-17701901" class=" external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-17701901" target="_blank"><code>HTMLImageElement</code></a>) interface, which provides special properties and methods (beyond the regular <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 object interface they also have available to them by inheritance) for manipulating the layout and presentation of input elements.
  */
public interface ImageElement extends Element {


  /**
    * Indicates the alignment of the image with respect to the surrounding context.
    */
  String getAlign();

  void setAlign(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img#attr-alt">alt</a></code>
 HTML attribute, indicating fallback context for the image.
    */
  String getAlt();

  void setAlt(String arg);


  /**
    * Width of the border around the image.
    */
  String getBorder();

  void setBorder(String arg);


  /**
    * True if the browser has fetched the image, and it is in a <a title="en/HTML/Element/Img#Image Format" rel="internal" href="https://developer.mozilla.org/En/HTML/Element/Img#Image_Format">supported image type</a> that was decoded without errors.
    */
  boolean isComplete();


  /**
    * The CORS setting for this image element. See <a title="en/HTML/CORS settings attributes" rel="internal" href="https://developer.mozilla.org/en/HTML/CORS_settings_attributes">CORS&nbsp;settings attributes</a> for details.
    */
  String getCrossOrigin();

  void setCrossOrigin(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img#attr-height">height</a></code>
 HTML attribute, indicating the rendered height of the image in CSS&nbsp;pixels.
    */
  int getHeight();

  void setHeight(int arg);


  /**
    * Space to the left and right of the image.
    */
  int getHspace();

  void setHspace(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img#attr-ismap">ismap</a></code>
 HTML attribute, indicating that the image is part of a server-side image map.
    */
  boolean isMap();

  void setIsMap(boolean arg);


  /**
    * URI of a long description of the image.
    */
  String getLongDesc();

  void setLongDesc(String arg);


  /**
    * A reference to a low-quality (but faster to load) copy of the image.
    */
  String getLowsrc();

  void setLowsrc(String arg);

  String getName();

  void setName(String arg);


  /**
    * Intrinsic height of the image in CSS&nbsp;pixels, if it is available; otherwise, 0.
    */
  int getNaturalHeight();


  /**
    * Intrinsic width of the image in CSS&nbsp;pixels, if it is available; otherwise, 0.
    */
  int getNaturalWidth();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element#attr-src">src</a></code>
 HTML attribute, containing the URL of the image.
    */
  String getSrc();

  void setSrc(String arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img#attr-usemap">usemap</a></code>
 HTML attribute, containing a partial URL of a map element.
    */
  String getUseMap();

  void setUseMap(String arg);


  /**
    * Space above and below the image.
    */
  int getVspace();

  void setVspace(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/img#attr-width">width</a></code>
 HTML attribute, indicating the rendered width of the image in CSS&nbsp;pixels.
    */
  int getWidth();

  void setWidth(int arg);

  int getX();

  int getY();
}
