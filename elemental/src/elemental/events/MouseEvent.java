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
import elemental.dom.Node;
import elemental.dom.Clipboard;
import elemental.html.Window;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;


import java.util.Date;

/**
  * The DOM&nbsp;<code>MouseEvent</code> represents events that occur due to the user interacting with a pointing device (such as a mouse). It's represented by the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsINSDOMMouseEvent&amp;ident=nsINSDOMMouseEvent" class="new">nsINSDOMMouseEvent</a></code>
&nbsp;interface, which extends the <code><a rel="internal" href="https://developer.mozilla.org/Article_not_found?uri=en/XPCOM_Interface_Reference/nsIDOMMouseEvent&amp;ident=nsIDOMMouseEvent" class="new">nsIDOMMouseEvent</a></code>
 interface.
  */
public interface MouseEvent extends UIEvent {
  /**
   * Contains the set of standard values returned by {@link #button}.
   */
  public interface Button {
    public static final short PRIMARY = 0;
    public static final short AUXILIARY = 1;
    public static final short SECONDARY = 2;
  }


  /**
    * <code>true</code> if the alt key was down when the mouse event was fired. <strong>Read only.</strong>
    */
  boolean isAltKey();


  /**
    * The button number that was pressed when the mouse event was fired:&nbsp;Left button=0, middle button=1 (if present), right button=2. For mice configured for left handed use in which the button actions are reversed the values are instead read from right to left. <strong>Read only.</strong>
    */
  int getButton();


  /**
    * The X coordinate of the mouse pointer in local (DOM content)&nbsp;coordinates. <strong>Read only.</strong>
    */
  int getClientX();


  /**
    * The Y coordinate of the mouse pointer in local (DOM content)&nbsp;coordinates. <strong>Read only.</strong>
    */
  int getClientY();


  /**
    * <code>true</code> if the control key was down when the mouse event was fired. <strong>Read only.</strong>
    */
  boolean isCtrlKey();

  Clipboard getDataTransfer();

  Node getFromElement();


  /**
    * <code>true</code> if the meta key was down when the mouse event was fired. <strong>Read only.</strong>
    */
  boolean isMetaKey();

  int getOffsetX();

  int getOffsetY();


  /**
    * The target to which the event applies. <strong>Read only.</strong>
    */
  EventTarget getRelatedTarget();


  /**
    * The X coordinate of the mouse pointer in global (screen)&nbsp;coordinates. <strong>Read only.</strong>
    */
  int getScreenX();


  /**
    * The Y coordinate of the mouse pointer in global (screen)&nbsp;coordinates. <strong>Read only.</strong>
    */
  int getScreenY();


  /**
    * <code>true</code> if the shift key was down when the mouse event was fired. <strong>Read only.</strong>
    */
  boolean isShiftKey();

  Node getToElement();

  int getWebkitMovementX();

  int getWebkitMovementY();

  int getX();

  int getY();

  void initMouseEvent(String type, boolean canBubble, boolean cancelable, Window view, int detail, int screenX, int screenY, int clientX, int clientY, boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey, int button, EventTarget relatedTarget);
}
