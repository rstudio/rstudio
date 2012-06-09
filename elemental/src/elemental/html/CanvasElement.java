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
  * DOM&nbsp;canvas elements expose the <code><a class="external" href="http://www.w3.org/TR/html5/the-canvas-element.html#htmlcanvaselement" rel="external nofollow" target="_blank" title="http://www.w3.org/TR/html5/the-canvas-element.html#htmlcanvaselement">HTMLCanvasElement</a></code> interface, which provides properties and methods for manipulating the layout and presentation of canvas elements. The <code>HTMLCanvasElement</code> interface inherits the properties and methods of the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/element">element</a></code>
 object interface.
  */
public interface CanvasElement extends Element {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas#attr-height">height</a></code>
 HTML attribute, specifying the height of the coordinate space in CSS pixels.
    */
  int getHeight();

  void setHeight(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/canvas#attr-width">width</a></code>
 HTML attribute, specifying the width of the coordinate space in CSS pixels.
    */
  int getWidth();

  void setWidth(int arg);


  /**
    * Returns a drawing context on the canvas, or null if the context ID is not supported. A drawing context lets you draw on the canvas. The currently accepted values are "2d" and "experimental-webgl". The "experimental-webgl" context is only available on browsers that implement <a title="En/WebGL" rel="internal" href="https://developer.mozilla.org/en/WebGL">WebGL</a>. Calling getContext with "2d" returns a <code><a href="https://developer.mozilla.org/en/DOM/CanvasRenderingContext2D" rel="internal">CanvasRenderingContext2D</a></code> Object, whereas calling it with "experimental-webgl" returns a <code>WebGLRenderingContext</code> Object.
    */
  CanvasRenderingContext getContext(String contextId);


  /**
    * <p>Returns a <code>data:</code> URL containing a representation of the image in the format specified by <code>type</code> (defaults to PNG).</p> <ul> <li>If the height or width of the canvas is 0, <code>"data:,</code>" representing the empty string, is returned.</li> <li>If the type requested is not <code>image/png</code>, and the returned value starts with <code>data:image/png</code>, then the requested type is not supported.</li> <li>Chrome supports the&nbsp;<code>image/webp&nbsp;</code>type.</li> <li>If the requested type is <code>image/jpeg&nbsp;</code>or&nbsp;<code>image/webp</code>, then the second argument, if it is between 0.0 and 1.0, is treated as indicating image quality; if the second argument is anything else, the default value for image quality is used. Other arguments are ignored.</li> </ul>
    */
  String toDataURL(String type);
}
