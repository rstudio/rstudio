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
import elemental.dom.NodeList;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * The <code>SVGSVGElement</code> interface provides access to the properties of <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 elements, as well as methods to manipulate them. This interface contains also various miscellaneous commonly-used utility methods, such as matrix operations and the ability to control the time of redraw on visual rendering devices.
  */
public interface SVGSVGElement extends SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable, SVGLocatable, SVGFitToViewBox, SVGZoomAndPan {


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/contentScriptType">contentScriptType</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element.
    */
  String getContentScriptType();

  void setContentScriptType(String arg);


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/contentStyleType">contentStyleType</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element.
    */
  String getContentStyleType();

  void setContentStyleType(String arg);


  /**
    * On an outermost <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element, this attribute indicates the current scale factor relative to the initial view to take into account user magnification and panning operations. DOM attributes <code>currentScale</code> and <code>currentTranslate</code> are equivalent to the 2x3 matrix <code>[a b c d e f] = [currentScale 0 0 currentScale currentTranslate.x currentTranslate.y]</code>. If "magnification" is enabled (i.e., <code>zoomAndPan="magnify"</code>), then the effect is as if an extra transformation were placed at the outermost level on the SVG document fragment (i.e., outside the outermost <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element).
    */
  float getCurrentScale();

  void setCurrentScale(float arg);


  /**
    * On an outermost <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element, the corresponding translation factor that takes into account user "magnification".
    */
  SVGPoint getCurrentTranslate();


  /**
    * The definition of the initial view (i.e., before magnification and panning) of the current innermost SVG document fragment. The meaning depends on the situation:<br> <ul> <li>If the initial view was a "standard" view, then: <ul> <li>the values for 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewBox" class="new">viewBox</a></code>, 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/zoomAndPan" class="new">zoomAndPan</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will match the values for the corresponding DOM attributes that are on <code>SVGSVGElement</code> directly</li> <li>the values for 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/transform">transform</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewTarget" class="new">viewTarget</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will be null</li> </ul> </li> <li>If the initial view was a link into a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/view">&lt;view&gt;</a></code>
 element, then: <ul> <li>the values for 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewBox" class="new">viewBox</a></code>, 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/zoomAndPan" class="new">zoomAndPan</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will correspond to the corresponding attributes for the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/view">&lt;view&gt;</a></code>
 element</li> <li>the values for 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/transform">transform</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewTarget" class="new">viewTarget</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will be null</li> </ul> </li> <li>If the initial view was a link into another element (i.e., other than a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/view">&lt;view&gt;</a></code>
), then: <ul> <li>the values for 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewBox" class="new">viewBox</a></code>, 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/zoomAndPan" class="new">zoomAndPan</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will match the values for the corresponding DOM attributes that are on <code>SVGSVGElement</code> directly for the closest ancestor <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element</li> <li>the values for 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/transform">transform</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will be null</li> <li>the 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewTarget" class="new">viewTarget</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will represent the target of the link</li> </ul> </li> <li>If the initial view was a link into the SVG document fragment using an SVG view specification fragment identifier (i.e., #svgView(...)), then: <ul> <li>the values for 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewBox" class="new">viewBox</a></code>, 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>, 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/zoomAndPan" class="new">zoomAndPan</a></code>, 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/transform">transform</a></code> and 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewTarget" class="new">viewTarget</a></code> within 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/currentView" class="new">currentView</a></code> will correspond to the values from the SVG view specification fragment identifier</li> </ul> </li> </ul>
    */
  SVGViewSpec getCurrentView();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/height">height</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedHeight();


  /**
    * Size of a pixel units (as defined by CSS2) along the x-axis of the viewport, which represents a unit somewhere in the range of 70dpi to 120dpi, and, on systems that support this, might actually match the characteristics of the target medium. On systems where it is impossible to know the size of a pixel, a suitable default pixel size is provided.
    */
  float getPixelUnitToMillimeterX();


  /**
    * Corresponding size of a pixel unit along the y-axis of the viewport.
    */
  float getPixelUnitToMillimeterY();


  /**
    * User interface (UI) events in DOM Level 2 indicate the screen positions at which the given UI event occurred. When the browser actually knows the physical size of a "screen unit", this attribute will express that information; otherwise, user agents will provide a suitable default value such as .28mm.
    */
  float getScreenPixelToMillimeterX();


  /**
    * Corresponding size of a screen pixel along the y-axis of the viewport.
    */
  float getScreenPixelToMillimeterY();


  /**
    * The initial view (i.e., before magnification and panning) of the current innermost SVG document fragment can be either the "standard" view (i.e., based on attributes on the <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element such as 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/viewBox" class="new">viewBox</a></code>) or to a "custom" view (i.e., a hyperlink into a particular <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/view">&lt;view&gt;</a></code>
 or other element). If the initial view is the "standard" view, then this attribute is false. If the initial view is a "custom" view, then this attribute is true.
    */
  boolean isUseCurrentView();


  /**
    * The position and size of the viewport (implicit or explicit) that corresponds to this <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element. When the browser is actually rendering the content, then the position and size values represent the actual values when rendering. The position and size values are unitless values in the coordinate system of the parent element. If no parent element exists (i.e., <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element represents the root of the document tree), if this SVG document is embedded as part of another document (e.g., via the HTML <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/object">&lt;object&gt;</a></code>
 element), then the position and size are unitless values in the coordinate system of the parent document. (If the parent uses CSS or XSL layout, then unitless values represent pixel units for the current CSS or XSL viewport.)
    */
  SVGRect getViewport();


  /**
    * Corresponds to attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/width">width</a></code> on the given <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element.
    */
  SVGAnimatedLength getAnimatedWidth();

  SVGAnimatedLength getX();

  SVGAnimatedLength getY();


  /**
    * Returns true if this SVG document fragment is in a paused state.
    */
  boolean animationsPaused();


  /**
    * Returns true if the rendered content of the given element is entirely contained within the supplied rectangle. Each candidate graphics element is to be considered a match only if the same graphics element can be a target of pointer events as defined in 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/pointer-events">pointer-events</a></code> processing.
    */
  boolean checkEnclosure(SVGElement element, SVGRect rect);


  /**
    * Returns true if the rendered content of the given element intersects the supplied rectangle. Each candidate graphics element is to be considered a match only if the same graphics element can be a target of pointer events as defined in 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/pointer-events">pointer-events</a></code> processing.
    */
  boolean checkIntersection(SVGElement element, SVGRect rect);


  /**
    * Creates an <code>SVGAngle</code> object outside of any document trees. The object is initialized to a value of zero degrees (unitless).
    */
  SVGAngle createSVGAngle();


  /**
    * Creates an <code>SVGLength</code> object outside of any document trees. The object is initialized to a value of zero user units.
    */
  SVGLength createSVGLength();


  /**
    * Creates an <code>SVGMatrix</code> object outside of any document trees. The object is initialized to the identity matrix.
    */
  SVGMatrix createSVGMatrix();


  /**
    * Creates an <code>SVGNumber</code> object outside of any document trees. The object is initialized to a value of zero.
    */
  SVGNumber createSVGNumber();


  /**
    * Creates an <code>SVGPoint</code> object outside of any document trees. The object is initialized to the point (0,0) in the user coordinate system.
    */
  SVGPoint createSVGPoint();


  /**
    * Creates an <code>SVGRect</code> object outside of any document trees. The object is initialized such that all values are set to 0 user units.
    */
  SVGRect createSVGRect();


  /**
    * Creates an <code>SVGTransform</code> object outside of any document trees. The object is initialized to an identity matrix transform (<code>SVG_TRANSFORM_MATRIX</code>).
    */
  SVGTransform createSVGTransform();


  /**
    * Creates an <code>SVGTransform</code> object outside of any document trees. The object is initialized to the given matrix transform (i.e., <code>SVG_TRANSFORM_MATRIX</code>). The values from the parameter matrix are copied, the matrix parameter is not adopted as <code>SVGTransform::matrix</code>.
    */
  SVGTransform createSVGTransformFromMatrix(SVGMatrix matrix);


  /**
    * Unselects any selected objects, including any selections of text strings and type-in bars.
    */
  void deselectAll();


  /**
    * In rendering environments supporting interactivity, forces the user agent to immediately redraw all regions of the viewport that require updating.
    */
  void forceRedraw();


  /**
    * Returns the current time in seconds relative to the start time for the current SVG document fragment. If getCurrentTime is called before the document timeline has begun (for example, by script running in a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/script">&lt;script&gt;</a></code>
 element before the document's SVGLoad event is dispatched), then 0 is returned.
    */
  float getCurrentTime();


  /**
    * Searches this SVG document fragment (i.e., the search is restricted to a subset of the document tree) for an Element whose id is given by <em>elementId</em>. If an Element is found, that Element is returned. If no such element exists, returns null. Behavior is not defined if more than one element has this id.
    */
  Element getElementById(String elementId);


  /**
    * Returns the list of graphics elements whose rendered content is entirely contained within the supplied rectangle. Each candidate graphics element is to be considered a match only if the same graphics element can be a target of pointer events as defined in 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/pointer-events">pointer-events</a></code> processing.
    */
  NodeList getEnclosureList(SVGRect rect, SVGElement referenceElement);


  /**
    * Returns the list of graphics elements whose rendered content intersects the supplied rectangle. Each candidate graphics element is to be considered a match only if the same graphics element can be a target of pointer events as defined in 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/pointer-events">pointer-events</a></code> processing.
    */
  NodeList getIntersectionList(SVGRect rect, SVGElement referenceElement);


  /**
    * Suspends (i.e., pauses) all currently running animations that are defined within the SVG document fragment corresponding to this <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/svg">&lt;svg&gt;</a></code>
 element, causing the animation clock corresponding to this document fragment to stand still until it is unpaused.
    */
  void pauseAnimations();


  /**
    * Adjusts the clock for this SVG document fragment, establishing a new current time. If <code>setCurrentTime</code> is called before the document timeline has begun (for example, by script running in a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/script">&lt;script&gt;</a></code>
 element before the document's SVGLoad event is dispatched), then the value of seconds in the last invocation of the method gives the time that the document will seek to once the document timeline has begun.
    */
  void setCurrentTime(float seconds);


  /**
    * <p>Takes a time-out value which indicates that redraw shall not occur until:</p> <ol> <li>the corresponding unsuspendRedraw() call has been made,</li> <li>an unsuspendRedrawAll() call has been made, or</li> <li>its timer has timed out.</li> </ol> <p>In environments that do not support interactivity (e.g., print media), then redraw shall not be suspended. Calls to <code>suspendRedraw()</code> and <code>unsuspendRedraw()</code> should, but need not be, made in balanced pairs.</p> <p>To suspend redraw actions as a collection of SVG DOM changes occur, precede the changes to the SVG DOM with a method call similar to:</p> <p><code>suspendHandleID = suspendRedraw(maxWaitMilliseconds);</code></p> <p>and follow the changes with a method call similar to:</p> <p><code>unsuspendRedraw(suspendHandleID);</code></p> <p>Note that multiple suspendRedraw calls can be used at once and that each such method call is treated independently of the other suspendRedraw method calls.</p>
    */
  int suspendRedraw(int maxWaitMilliseconds);


  /**
    * Unsuspends (i.e., unpauses) currently running animations that are defined within the SVG document fragment, causing the animation clock to continue from the time at which it was suspended.
    */
  void unpauseAnimations();


  /**
    * Cancels a specified <code>suspendRedraw()</code> by providing a unique suspend handle ID that was returned by a previous <code>suspendRedraw()</code> call.
    */
  void unsuspendRedraw(int suspendHandleId);


  /**
    * Cancels all currently active <code>suspendRedraw()</code> method calls. This method is most useful at the very end of a set of SVG DOM calls to ensure that all pending <code>suspendRedraw()</code> method calls have been cancelled.
    */
  void unsuspendRedrawAll();
}
