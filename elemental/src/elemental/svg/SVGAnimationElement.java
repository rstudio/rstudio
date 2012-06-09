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
  * The <code>SVGAnimationElement</code> interface is the base interface for all of the animation element interfaces: <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGAnimateElement">SVGAnimateElement</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGSetElement">SVGSetElement</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGAnimateColorElement">SVGAnimateColorElement</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGAnimateMotionElement">SVGAnimateMotionElement</a></code>
 and <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/SVGAnimateTransformElement">SVGAnimateTransformElement</a></code>
.
  */
public interface SVGAnimationElement extends SVGElement, SVGTests, SVGExternalResourcesRequired, ElementTimeControl {


  /**
    * The element which is being animated.
    */
  SVGElement getTargetElement();


  /**
    * Returns the current time in seconds relative to time zero for the given time container.
    */
  float getCurrentTime();


  /**
    * Returns the number of seconds for the simple duration for this animation. If the simple duration is undefined (e.g., the end time is indefinite), then a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code NOT_SUPPORTED_ERR is raised.
    */
  float getSimpleDuration();


  /**
    * Returns the begin time, in seconds, for this animation element's current interval, if it exists, regardless of whether the interval has begun yet. If there is no current interval, then a <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/DOMException">DOMException</a></code>
 with code INVALID_STATE_ERR is thrown.
    */
  float getStartTime();
}
