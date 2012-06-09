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
package elemental.events;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * A <code>Touch</code> object represents a single point of contact between the user and a touch-sensitive interface device (which may be, for example, a touchscreen or a trackpad).
  */
public interface Touch {


  /**
    * The X coordinate of the touch point relative to the viewport, not including any scroll offset. <strong>Read only.</strong>
    */
  int getClientX();


  /**
    * The Y coordinate of the touch point relative to the viewport, not including any scroll offset. <strong>Read only.</strong>
    */
  int getClientY();


  /**
    * A unique identifier for this <code>Touch</code> object. A given touch (say, by a finger) will have the same identifier for the duration of its movement around the surface. This lets you ensure that you're tracking the same touch all the time. <strong>Read only.</strong>
    */
  int getIdentifier();


  /**
    * The X coordinate of the touch point relative to the viewport, including any scroll offset. <strong>Read only.</strong>
    */
  int getPageX();


  /**
    * The Y coordinate of the touch point relative to the viewport, including any scroll offset. <strong>Read only.</strong>
    */
  int getPageY();


  /**
    * The X coordinate of the touch point relative to the screen, not including any scroll offset. <strong>Read only.</strong>
    */
  int getScreenX();


  /**
    * The Y coordinate of the touch point relative to the screen, not including any scroll offset. <strong>Read only.</strong>
    */
  int getScreenY();

  EventTarget getTarget();

  float getWebkitForce();

  int getWebkitRadiusX();

  int getWebkitRadiusY();

  float getWebkitRotationAngle();
}
