/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.editor.client.adapters.TakesValueEditor;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * A <code>ToggleButton</code> is a stylish stateful button which allows the
 * user to toggle between <code>up</code> and <code>down</code> states.
 * 
 * <p>
 * <img class='gallery' src='doc-files/ToggleButton.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>
 * .gwt-ToggleButton-up/down/up-hovering/down-hovering/up-disabled/down-disabled
 * {.html-face}</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.ToggleButtonExample}
 * </p>
 */
public class ToggleButton extends CustomButton implements HasValue<Boolean>, IsEditor<LeafValueEditor<Boolean>> {
  private static String STYLENAME_DEFAULT = "gwt-ToggleButton";

  private LeafValueEditor<Boolean> editor;

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
   * Constructor for <code>ToggleButton</code>. The supplied image is used to
   * construct the default face of the button.
   * 
   * @param upImage image for the default (up) face of the button
   * @param handler the click handler
   */
  public ToggleButton(Image upImage, ClickHandler handler) {
    super(upImage, handler);
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied image is used to
   * construct the default face of the button.
   * 
   * @param upImage image for the default (up) face of the button
   * @param listener the click listener
   * @deprecated Use {@link #ToggleButton(Image, ClickHandler)} instead
   */
  @Deprecated
  public ToggleButton(Image upImage, ClickListener listener) {
    super(upImage, listener);
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
   * @param handler the click handler
   */
  public ToggleButton(Image upImage, Image downImage, ClickHandler handler) {
    super(upImage, downImage, handler);
  }

  /**
   * Constructor for <code>ToggleButton</code>.
   * 
   * @param upImage image for the default(up) face of the button
   * @param downImage image for the down face of the button
   * @param listener clickListener
   * @deprecated Use {@link #ToggleButton(Image, Image, ClickHandler)} instead
   */
  @Deprecated
  public ToggleButton(Image upImage, Image downImage, ClickListener listener) {
    super(upImage, downImage, listener);
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
   * @param handler the click handler
   */
  public ToggleButton(String upText, ClickHandler handler) {
    super(upText, handler);
  }

  /**
   * Constructor for <code>ToggleButton</code>. The supplied text is used to
   * construct the default face of the button.
   * 
   * @param upText the text for the default (up) face of the button
   * @param listener the click listener
   * @deprecated Use {@link #ToggleButton(String, ClickHandler)} instead
   */
  @Deprecated
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

  /**
   * Constructor for <code>ToggleButton</code>.
   * 
   * @param upText the text for the default (up) face of the button
   * @param downText the text for down face of the button
   * @param handler the click handler
   */
  public ToggleButton(String upText, String downText, ClickHandler handler) {
    super(upText, downText, handler);
  }

  public HandlerRegistration addValueChangeHandler(
      ValueChangeHandler<Boolean> handler) {
    return addHandler(handler, ValueChangeEvent.getType());
  }

  public LeafValueEditor<Boolean> asEditor() {
    if (editor == null) {
      editor = TakesValueEditor.of(this);
    }
    return editor;
  }

  /**
   * Determines whether this button is currently down.
   * 
   * @return <code>true</code> if the button is pressed, false otherwise. Will
   *         not return null
   */
  public Boolean getValue() {
    return isDown();
  }

  @Override
  public boolean isDown() {
    // Changes access to public.
    return super.isDown();
  }

  /**
   * {@inheritDoc} Does not fire {@link ValueChangeEvent}. (If you want the
   * event to fire, use {@link #setValue(Boolean, boolean)})
   */
  @Override
  public void setDown(boolean down) {
    // Changes access to public.
    super.setDown(down);
  }

  /**
   * Sets whether this button is down.
   * 
   * @param value true to press the button, false otherwise; null value implies
   *          false
   */
  public void setValue(Boolean value) {
    setValue(value, false);
  }

  /**
   * Sets whether this button is down, firing {@link ValueChangeEvent} if
   * appropriate.
   * 
   * @param value true to press the button, false otherwise; null value implies
   *          false
   * @param fireEvents If true, and value has changed, fire a
   *          {@link ValueChangeEvent}
   */
  public void setValue(Boolean value, boolean fireEvents) {
    if (value == null) {
      value = Boolean.FALSE;
    }
    boolean oldValue = isDown();
    setDown(value);
    if (fireEvents) {
      ValueChangeEvent.fireIfNotEqual(this, oldValue, value);
    }
  }

  @Override
  protected void onClick() {
    toggleDown();
    super.onClick();
    ValueChangeEvent.fire(this, isDown());
  }
}
