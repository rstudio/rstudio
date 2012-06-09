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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * DOM <code>video</code> objects expose the <a class="external" title="http://www.w3.org/TR/html5/video.html#htmlvideoelement" rel="external" href="http://www.w3.org/TR/html5/video.html#htmlvideoelement" target="_blank">HTMLVideoElement</a> interface, which provides special properties (beyond the regular <a href="https://developer.mozilla.org/en/DOM/element" rel="internal">element</a> object and <a title="en/DOM/HTMLMediaElement" rel="internal" href="https://developer.mozilla.org/en/DOM/HTMLMediaElement">HTMLMediaElement</a> interfaces they also have available to them by inheritance) for manipulating video objects.
  */
public interface VideoElement extends MediaElement {


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-height">height</a></code>
 HTML attribute, which specifies the height of the display area, in CSS pixels.
    */
  int getHeight();

  void setHeight(int arg);


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-poster">poster</a></code>
 HTML&nbsp;attribute, which specifies an image to show while no video data is available.
    */
  String getPoster();

  void setPoster(String arg);


  /**
    * The intrinsic height of the resource in CSS pixels, taking into account the dimensions, aspect ratio, clean aperture, resolution, and so forth, as defined for the format used by the resource. If the element's ready state is HAVE_NOTHING, the value is 0.
    */
  int getVideoHeight();


  /**
    * The intrinsic width of the resource in CSS pixels, taking into account the dimensions, aspect ratio, clean aperture, resolution, and so forth, as defined for the format used by the resource. If the element's ready state is HAVE_NOTHING, the value is 0.
    */
  int getVideoWidth();

  int getWebkitDecodedFrameCount();

  boolean isWebkitDisplayingFullscreen();

  int getWebkitDroppedFrameCount();

  boolean isWebkitSupportsFullscreen();


  /**
    * Reflects the 

<code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/video#attr-width">width</a></code>
&nbsp;HTML&nbsp;attribute, which specifies the width of the display area, in CSS pixels.
    */
  int getWidth();

  void setWidth(int arg);

  void webkitEnterFullScreen();

  void webkitEnterFullscreen();

  void webkitExitFullScreen();

  void webkitExitFullscreen();
}
