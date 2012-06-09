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
import elemental.html.Window;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>A <code>TouchEvent</code> represents an event sent when the state of contacts with a touch-sensitive surface changes. This surface can be a touch screen or trackpad, for example. The event can describe one or more points of contact with the screen and includes support for detecting movement, addition and removal of contact points, and so forth.</p>
<p>Touches are represented by the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
&nbsp;object; each touch is described by a position, size and shape, amount of pressure, and target element. Lists of touches are represented by <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TouchList">TouchList</a></code>
 objects.</p>
  */
public interface TouchEvent extends UIEvent {


  /**
    * A Boolean value indicating whether or not the alt key was down when the touch event was fired. <strong>Read only.</strong>
    */
  boolean isAltKey();


  /**
    * A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TouchList">TouchList</a></code>
 of all the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
 objects representing individual points of contact whose states changed between the previous touch event and this one. <strong>Read only.</strong>
    */
  TouchList getChangedTouches();


  /**
    * A Boolean value indicating whether or not the control key was down when the touch event was fired. <strong>Read only.</strong>
    */
  boolean isCtrlKey();


  /**
    * A Boolean value indicating whether or not the meta key was down when the touch event was fired. <strong>Read only.</strong>
    */
  boolean isMetaKey();


  /**
    * A Boolean value indicating whether or not the shift key was down when the touch event was fired. <strong>Read only.</strong>
    */
  boolean isShiftKey();


  /**
    * A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TouchList">TouchList</a></code>
 of all the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
&nbsp;objects that are both currently in contact with the touch surface <strong>and</strong> were also started on the same element that is the target of the event. <strong>Read only.</strong>
    */
  TouchList getTargetTouches();


  /**
    * A <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/TouchList">TouchList</a></code>
 of all the <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/Touch">Touch</a></code>
&nbsp;objects representing all current points of contact with the surface, regardless of target or changed status. <strong>Read only.</strong>
    */
  TouchList getTouches();

  void initTouchEvent(TouchList touches, TouchList targetTouches, TouchList changedTouches, String type, Window view, int screenX, int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey);
}
