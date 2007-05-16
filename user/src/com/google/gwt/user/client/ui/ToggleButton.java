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
 * A <code>ToggleButton</code> is a stylish stateful button which allows the
 * user to toggle between <code>up</code> and <code>down</code> states.
 * 
 * <p>
 * <img class='gallery' src='ToggleButton.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-ToggleButton-up/down/up-hovering/down-hovering/up-disabled/down-disabled {.html-face}</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.ToggleButtonExample}
 * </p>
 */
public class ToggleButton extends CustomButton {
  private static String STYLENAME_DEFAULT = "gwt-ToggleButton";

  {
    setStyleName(STYLENAME_DEFAULT);
  }

  /**
   * Constructor for <code>ToggleButton</code>.
   */
  public ToggleButton() {
    super();
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied image is used to
   * construct the default face.
   * 
   * @param upImage image for the default face of the button
   */
  public ToggleButton(Image upImage) {
    super(upImage);
  }

  /**
   * Constructor for <code>ToggleButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   * @param downImage image for the down face of the button
   */
  public ToggleButton(Image upImage, Image downImage) {
    super(upImage, downImage);
  }

  /**
   * Constructor for <code>ToggleButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   * @param downImage image for the down face of the button
   * @param listener clickListener
   */
  public ToggleButton(Image upImage, Image downImage, ClickListener listener) {
    super(upImage, downImage, listener);
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied image is used to
   * construct the default face of the button.
   * 
   * @param upImage image for the default (up) face of the button
   * @param listener the click listener
   */
  public ToggleButton(Image upImage, ClickListener listener) {
    super(upImage, listener);
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied text is used to
   * construct the default face of the button.
   * 
   * @param upText the text for the default (up) face of the button.
   */
  public ToggleButton(String upText) {
    super(upText);
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied text is used to
   * construct the default face of the button.
   * 
   * @param upText the text for the default (up) face of the button
   * @param listener the click listener
   */
  public ToggleButton(String upText, ClickListener listener) {
    super(upText, listener);
  }

  /**
   * Constructor for <code>ToggleButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param downText the text for down face of the button
   */
  public ToggleButton(String upText, String downText) {
    super(upText, downText);
  }

  public boolean isDown() {
    return super.isDown();
  }

  public void onBrowserEvent(Event event) {
    if (isEnabled() == false) {
      return;
    }
    int type = DOM.eventGetType(event);
    switch (type) {
      case Event.ONCLICK:
        toggleDown();
        break;
    }
    super.onBrowserEvent(event);
  }

  public void setDown(boolean isDown) {
    super.setDown(isDown);
  }
}
