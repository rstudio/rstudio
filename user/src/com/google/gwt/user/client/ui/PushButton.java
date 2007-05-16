/*
 * Copyright 2006 Google Inc.
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

package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * A normal push button with custom styling.
 * 
 * <p>
 * <img class='gallery' src='PushButton.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-PushButton-up/down/up-hovering/down-hovering/up-disabled/down-disabled {.html-face}</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.PushButtonExample}
 * </p>
 */
public class PushButton extends CustomButton {

  private static final String STYLENAME_DEFAULT = "gwt-PushButton";

  private boolean waitingForMouseUp = false;

  {
    setStyleName(STYLENAME_DEFAULT);
  }

  /**
   * Constructor for <code>PushButton</code>.
   */
  public PushButton() {
    super();
  }

  /**
   * Constructor for <code>PushButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   */
  public PushButton(Image upImage) {
    super(upImage);
  }

  /**
   * Constructor for <code>PushButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   * @param downImage image for the down face of the button
   */
  public PushButton(Image upImage, Image downImage) {
    super(upImage, downImage);
  }

  /**
   * Constructor for <code>PushButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   * @param downImage image for the down face of the button
   * @param listener clickListener
   */
  public PushButton(Image upImage, Image downImage, ClickListener listener) {
    super(upImage, listener);
  }

  /**
   * Constructor for <code>PushButton</code>. The supplied image is used to
   * construct the default face of the button.
   * 
   * @param upImage image for the default (up) face of the button
   * @param listener the click listener
   */
  public PushButton(Image upImage, ClickListener listener) {
    super(upImage, listener);
  }

  /**
   * Constructor for <code>PushButton</code>. The supplied text is used to
   * construct the default face of the button.
   * 
   * @param upText the text for the default (up) face of the button.
   */
  public PushButton(String upText) {
    super(upText);
  }

  /**
   * Constructor for <code>PushButton</code>. The supplied text is used to
   * construct the default face of the button.
   * 
   * @param upText the text for the default (up) face of the button
   * @param listener the click listener
   */
  public PushButton(String upText, ClickListener listener) {
    super(upText, listener);
  }

  /**
   * Constructor for <code>PushButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param downText the text for down face of the button
   */
  public PushButton(String upText, String downText) {
    super(upText, downText);
  }

  /**
   * Constructor for <code>PushButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param downText the text for down face of the button
   * @param listener the click listener
   */
  public PushButton(String upText, String downText, ClickListener listener) {
    super(upText, downText, listener);
  }
  public void onBrowserEvent(Event event) {
    // Should not act on button if the button is disabled. This can happen
    // because an event is bubbled up from a non-disabled interior component.
    if (isEnabled() == false) {
      return;
    }
    int type = DOM.eventGetType(event);
    switch (type) {
      case Event.ONMOUSEDOWN:
        waitingForMouseUp = true;
        setDown(true);
        break;
      case Event.ONCLICK:
        // Must synthesize click events because when we have two separate face
        // elements for up/down, no click events are generated.
        return;
      case Event.ONMOUSEUP:
        if (waitingForMouseUp) {
          fireClickListeners();
        }
        waitingForMouseUp = false;
        setDown(false);
        break;
      case Event.ONMOUSEOUT:
        setDown(false);
        break;
      case Event.ONMOUSEOVER:
        if (waitingForMouseUp) {
          setDown(true);
        }
    }
    super.onBrowserEvent(event);
  }
}
