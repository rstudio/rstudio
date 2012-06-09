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
import elemental.dom.Clipboard;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;


import java.util.Date;

/**
  * <p>This chapter describes the DOM Event Model. The <a class="external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-Event" title="http://www.w3.org/TR/DOM-Level-2-Events/events.html#Events-Event" target="_blank">Event</a> interface itself is described, as well as the interfaces for event registration on nodes in the DOM, and <a title="en/DOM/element.addEventListener" rel="internal" href="https://developer.mozilla.org/en/DOM/element.addEventListener">event listeners</a>, and several longer examples that show how the various event interfaces relate to one another.</p>
<p>There is an excellent diagram that clearly explains the three phases of event flow through the DOM in the <a class="external" title="http://www.w3.org/TR/DOM-Level-3-Events/#dom-event-architecture" rel="external" href="http://www.w3.org/TR/DOM-Level-3-Events/#dom-event-architecture" target="_blank">DOM Level 3 Events draft</a>.</p>
  */
public interface Event {
public static final String CLICK = "click";
public static final String CONTEXTMENU = "contextmenu";
public static final String DBLCLICK = "dblclick";
public static final String CHANGE = "change";
public static final String MOUSEDOWN = "mousedown";
public static final String MOUSEMOVE = "mousemove";
public static final String MOUSEOUT = "mouseout";
public static final String MOUSEOVER = "mouseover";
public static final String MOUSEUP = "mouseup";
public static final String MOUSEWHEEL = "mousewheel";
public static final String FOCUS = "focus";
public static final String FOCUSIN = "focusin";
public static final String FOCUSOUT = "focusout";
public static final String BLUR = "blur";
public static final String KEYDOWN = "keydown";
public static final String KEYPRESS = "keypress";
public static final String KEYUP = "keyup";
public static final String SCROLL = "scroll";
public static final String BEFORECUT = "beforecut";
public static final String CUT = "cut";
public static final String BEFORECOPY = "beforecopy";
public static final String COPY = "copy";
public static final String BEFOREPASTE = "beforepaste";
public static final String PASTE = "paste";
public static final String DRAGENTER = "dragenter";
public static final String DRAGOVER = "dragover";
public static final String DRAGLEAVE = "dragleave";
public static final String DROP = "drop";
public static final String DRAGSTART = "dragstart";
public static final String DRAG = "drag";
public static final String DRAGEND = "dragend";
public static final String RESIZE = "resize";
public static final String SELECTSTART = "selectstart";
public static final String SUBMIT = "submit";
public static final String ERROR = "error";
public static final String WEBKITANIMATIONSTART = "webkitAnimationStart";
public static final String WEBKITANIMATIONITERATION = "webkitAnimationIteration";
public static final String WEBKITANIMATIONEND = "webkitAnimationEnd";
public static final String WEBKITTRANSITIONEND = "webkitTransitionEnd";
public static final String INPUT = "input";
public static final String INVALID = "invalid";
public static final String TOUCHSTART = "touchstart";
public static final String TOUCHMOVE = "touchmove";
public static final String TOUCHEND = "touchend";
public static final String TOUCHCANCEL = "touchcancel";


    static final int AT_TARGET = 2;

    static final int BUBBLING_PHASE = 3;

    static final int CAPTURING_PHASE = 1;

    static final int NONE = 0;


  /**
    * A boolean indicating whether the event bubbles up through the DOM or not.
    */
  boolean isBubbles();


  /**
    * A boolean indicating whether the bubbling of the event has been canceled or not.
    */
  boolean isCancelBubble();

  void setCancelBubble(boolean arg);


  /**
    * A boolean indicating whether the event is cancelable.
    */
  boolean isCancelable();

  Clipboard getClipboardData();


  /**
    * A reference to the currently registered target for the event.
    */
  EventTarget getCurrentTarget();


  /**
    * Indicates whether or not <code><a rel="custom" href="https://developer.mozilla.org/en/DOM/event.preventDefault">event.preventDefault()</a></code>
 has been called on the event.
    */
  boolean isDefaultPrevented();


  /**
    * Indicates which phase of the event flow is being processed.
    */
  int getEventPhase();

  boolean isReturnValue();

  void setReturnValue(boolean arg);

  EventTarget getSrcElement();


  /**
    * A reference to the target to which the event was originally dispatched.
    */
  EventTarget getTarget();


  /**
    * The time that the event was created.
    */
  double getTimeStamp();


  /**
    * The name of the event (case-insensitive).
    */
  String getType();


  /**
    * Initializes the value of an Event created through the <code>DocumentEvent</code> interface.
    */
  void initEvent(String eventTypeArg, boolean canBubbleArg, boolean cancelableArg);


  /**
    * Cancels the event (if it is cancelable).
    */
  void preventDefault();


  /**
    * For this particular event, no other listener will be called. Neither those attached on the same element, nor those attached on elements which will be traversed later (in capture phase, for instance)
    */
  void stopImmediatePropagation();


  /**
    * Stops the propagation of events further along in the DOM.
    */
  void stopPropagation();
}
